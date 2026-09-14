#!/usr/bin/env python3
"""
build.py -- one-command build helper for UnstableCompanions.

What this does:
  1. Checks that a Java 21 JDK is available (Fabric 1.21.11 requires it).
  2. Makes the Gradle wrapper executable (Linux/macOS).
  3. Runs the Gradle build.
  4. On failure: prints just the actual compiler errors (Gradle's full output
     is very noisy), with a short glossary of the most common Fabric/Minecraft
     error patterns and what they usually mean.
  5. On success: finds the built jar under build/libs/ and tells you exactly
     where it is, and optionally copies it straight into a Minecraft
     `mods` folder if you pass --mods-dir.

Usage:
    python build.py                     # just build, report the jar path
    python build.py --clean              # clean build (slower, use if things
                                          # get into a weird state)
    python build.py --mods-dir "C:/Users/you/AppData/Roaming/.minecraft/mods"
                                          # build AND copy the jar there
    python build.py --verbose            # show full Gradle output, unfiltered

This script does not require any pip packages -- only the Python standard
library, so it runs with whatever Python 3 you already have.
"""

import argparse
import os
import platform
import re
import shutil
import subprocess
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent
IS_WINDOWS = platform.system() == "Windows"
GRADLEW = PROJECT_ROOT / ("gradlew.bat" if IS_WINDOWS else "gradlew")


def _supports_color():
    if IS_WINDOWS:
        return os.environ.get("WT_SESSION") is not None or os.environ.get("TERM") is not None
    return sys.stdout.isatty()


COLOR = _supports_color()


def c(text, code):
    return f"\033[{code}m{text}\033[0m" if COLOR else text


def red(t): return c(t, "91")
def green(t): return c(t, "92")
def yellow(t): return c(t, "93")
def cyan(t): return c(t, "96")
def bold(t): return c(t, "1")


def banner(text):
    print()
    print(bold(cyan("=" * 70)))
    print(bold(cyan(text)))
    print(bold(cyan("=" * 70)))


# ---------------------------------------------------------------------------
# Step 1: Java version check
# ---------------------------------------------------------------------------

def find_java_version():
    """Returns (major_version_or_None, java_path_or_None)."""
    java_cmd = shutil.which("java")
    if not java_cmd:
        return None, None
    try:
        result = subprocess.run([java_cmd, "-version"], capture_output=True, text=True, timeout=15)
        output = result.stderr or result.stdout  # `java -version` prints to stderr
        match = re.search(r'version "(\d+)(\.\d+)?', output)
        if match:
            major = int(match.group(1))
            if major == 1:  # old-style "1.8.0_XXX" reporting
                match2 = re.search(r'version "1\.(\d+)', output)
                if match2:
                    major = int(match2.group(1))
            return major, java_cmd
        return None, java_cmd
    except Exception:
        return None, java_cmd


def check_java():
    banner("Step 1/3: Checking Java")
    major, java_cmd = find_java_version()

    if java_cmd is None:
        print(red("No 'java' command found on your PATH."))
        print("Install a Java 21 JDK (Temurin is a good free choice):")
        print(cyan("  https://adoptium.net/temurin/releases/?version=21"))
        print("After installing, restart your terminal/IDE so PATH updates.")
        return False

    print(f"Found Java at: {java_cmd}")

    if major is None:
        print(yellow("Could not determine the Java version -- proceeding anyway, "
                      "but if the build fails with 'class file version' errors, "
                      "that's almost certainly why."))
        return True

    print(f"Detected Java major version: {major}")

    if major < 21:
        print(red(f"Java {major} found, but this mod (Minecraft 1.21.11) requires Java 21+."))
        print("Install a Java 21 JDK and make sure it's the one on your PATH:")
        print(cyan("  https://adoptium.net/temurin/releases/?version=21"))
        if IS_WINDOWS:
            print("On Windows, check with: where java")
        else:
            print("Check with: which -a java   (and update PATH/JAVA_HOME if needed)")
        return False

    print(green("Java version OK."))
    return True


# ---------------------------------------------------------------------------
# Step 2: Gradle wrapper
# ---------------------------------------------------------------------------

def prepare_gradlew():
    banner("Step 2/3: Preparing Gradle wrapper")

    if not GRADLEW.exists():
        print(red(f"Could not find {GRADLEW.name} in {PROJECT_ROOT}."))
        print("Make sure you're running this script from inside the unzipped "
              "UnstableCompanions project folder.")
        return False

    wrapper_jar = PROJECT_ROOT / "gradle" / "wrapper" / "gradle-wrapper.jar"
    if not wrapper_jar.exists():
        print(red(f"Missing {wrapper_jar} -- the Gradle wrapper is incomplete."))
        print("Re-download/unzip the project; gradle/wrapper/gradle-wrapper.jar "
              "must be present alongside gradlew.")
        return False

    if not IS_WINDOWS:
        try:
            current_mode = GRADLEW.stat().st_mode
            GRADLEW.chmod(current_mode | 0o111)  # add execute bits
            print(green(f"Made {GRADLEW.name} executable."))
        except Exception as e:
            print(yellow(f"Could not chmod {GRADLEW.name} ({e}); trying to run it anyway."))
    else:
        print(f"Using {GRADLEW.name}.")

    return True


# ---------------------------------------------------------------------------
# Step 3: Run the build
# ---------------------------------------------------------------------------

ERROR_PATTERNS = [
    re.compile(r"error:", re.IGNORECASE),
    re.compile(r"^e: ", re.IGNORECASE),
    re.compile(r"cannot find symbol"),
    re.compile(r"cannot be applied to given types"),
    re.compile(r"incompatible types"),
    re.compile(r"package .* does not exist"),
    re.compile(r"class file has wrong version"),
    re.compile(r"unmappable character"),
    re.compile(r"BUILD FAILED"),
    re.compile(r"FAILURE: Build failed"),
    re.compile(r"\* What went wrong"),
    re.compile(r"Caused by:"),
]

COMMON_FIXES = [
    (re.compile(r"class file has wrong version", re.IGNORECASE),
     "This means your Gradle build is running with the wrong Java version. "
     "Confirm 'java -version' reports 21. If you have multiple JDKs "
     "installed, set JAVA_HOME to the Java 21 one and re-run."),
    (re.compile(r"cannot find symbol", re.IGNORECASE),
     "A method/class name doesn't match what this Minecraft version expects. "
     "Copy the exact error (file name + line number) and share it -- these "
     "are one-line fixes once we see the exact message."),
    (re.compile(r"package .* does not exist", re.IGNORECASE),
     "An import path is wrong or a dependency isn't resolved. Try running "
     "with --clean once; if it persists, check your internet connection, "
     "since Gradle needs to download Minecraft/Fabric files on first build."),
    (re.compile(r"Could not resolve", re.IGNORECASE),
     "Gradle couldn't download something it needs (Minecraft, Fabric API, "
     "mappings...). Check your internet connection and that no firewall/VPN "
     "is blocking maven.fabricmc.net or Mojang's servers."),
    (re.compile(r"Unsupported class file major version", re.IGNORECASE),
     "Gradle itself is running on the wrong Java version (separate from the "
     "one your project compiles with). Set JAVA_HOME to a Java 21 JDK before "
     "running this script."),
]


def run_build(clean, verbose):
    banner("Step 3/3: Building the mod")
    print("This can take a while the first time (Gradle downloads Minecraft, "
          "Fabric Loader, and mappings). Subsequent builds are much faster.\n")

    cmd = [str(GRADLEW)]
    if clean:
        cmd.append("clean")
    cmd.append("build")

    print(f"Running: {' '.join(cmd)}\n")

    process = subprocess.Popen(
        cmd,
        cwd=str(PROJECT_ROOT),
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
    )

    all_lines = []
    for line in process.stdout:
        line = line.rstrip("\n")
        all_lines.append(line)
        if verbose:
            print(line)
        else:
            if line.strip():
                sys.stdout.write(".")
                sys.stdout.flush()

    process.wait()
    if not verbose:
        print()

    success = process.returncode == 0

    if success:
        print(green("\nBUILD SUCCEEDED"))
        return True, all_lines
    else:
        print(red("\nBUILD FAILED"))
        if not verbose:
            print_filtered_errors(all_lines)
        return False, all_lines


def print_filtered_errors(all_lines):
    banner("Relevant error output")

    matched_indices = set()
    for i, line in enumerate(all_lines):
        if any(p.search(line) for p in ERROR_PATTERNS):
            for j in range(max(0, i - 1), min(len(all_lines), i + 3)):
                matched_indices.add(j)

    if not matched_indices:
        print(yellow("Could not automatically identify the specific error line(s)."))
        print("Re-run with --verbose to see the full Gradle output:")
        print(cyan("  python build.py --verbose"))
        return

    last_printed = -2
    for i in sorted(matched_indices):
        if i != last_printed + 1:
            print(c("  ...", "90"))
        print(f"  {all_lines[i]}")
        last_printed = i

    full_text = "\n".join(all_lines)
    suggestions = [msg for pattern, msg in COMMON_FIXES if pattern.search(full_text)]
    if suggestions:
        print()
        print(bold("Likely cause / next step:"))
        for s in suggestions:
            print(f"  - {s}")

    print()
    print("If this isn't enough context, re-run with --verbose and share the "
          "full output -- paste it back and the exact fix is usually a "
          "one-line change.")


# ---------------------------------------------------------------------------
# Post-build: locate and optionally copy the jar
# ---------------------------------------------------------------------------

def find_output_jar():
    libs_dir = PROJECT_ROOT / "build" / "libs"
    if not libs_dir.exists():
        return None
    candidates = [p for p in libs_dir.glob("*.jar") if "-sources" not in p.name and "-dev" not in p.name]
    if not candidates:
        candidates = list(libs_dir.glob("*.jar"))
    if not candidates:
        return None
    return sorted(candidates, key=lambda p: len(p.name))[0]


def handle_output(mods_dir_arg):
    jar = find_output_jar()
    if jar is None:
        print(yellow("Build reported success but no jar was found under build/libs/. "
                      "Something unusual happened -- check build/libs/ manually."))
        return

    banner("Done")
    print(f"Your mod jar is at:\n  {jar}")

    if mods_dir_arg:
        mods_dir = Path(mods_dir_arg).expanduser()
        if not mods_dir.exists():
            print(yellow(f"\n--mods-dir path does not exist: {mods_dir}"))
            print("Not copying automatically -- create that folder first, or "
                  "copy the jar there yourself.")
            return
        dest = mods_dir / jar.name
        shutil.copy2(jar, dest)
        print(green(f"\nCopied to: {dest}"))
        print("Remember: Fabric API must also be in that mods folder, and you "
              "need Fabric Loader installed for Minecraft 1.21.11.")
    else:
        print("\nTo use it: copy this jar into your Minecraft 'mods' folder "
              "(alongside Fabric API), or re-run this script with "
              "--mods-dir \"<path to your mods folder>\" to do that automatically.")


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description="Build UnstableCompanions and report results clearly.")
    parser.add_argument("--clean", action="store_true", help="Run a clean build (slower, use if the build is in a weird state).")
    parser.add_argument("--verbose", action="store_true", help="Show the full, unfiltered Gradle output.")
    parser.add_argument("--mods-dir", type=str, default=None,
                         help="If the build succeeds, copy the jar directly into this Minecraft mods folder.")
    parser.add_argument("--skip-java-check", action="store_true", help="Skip the Java version check (advanced).")
    args = parser.parse_args()

    print(bold(cyan("UnstableCompanions build helper")))

    if not args.skip_java_check:
        if not check_java():
            sys.exit(1)

    if not prepare_gradlew():
        sys.exit(1)

    success, _ = run_build(clean=args.clean, verbose=args.verbose)

    if success:
        handle_output(args.mods_dir)
        sys.exit(0)
    else:
        sys.exit(1)


if __name__ == "__main__":
    main()
