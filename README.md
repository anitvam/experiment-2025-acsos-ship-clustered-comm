# Experiments for Robust Communication through Collective Adaptive Relay Schemes for Maritime Vessels
### Submitted for [ACSOS 2025](https://conf.researchr.org/home/acsos-2025) conference

## Reproduce the entire experiment

**WARNING**: re-running the whole experiment may take a very long time on a normal computer.

### Reproduce with containers (recommended)

1. Install docker and docker-compose
2. Run `docker-compose up`
3. The charts will be available in the `charts` folder.

### Reproduce natively

1. Install a Gradle-compatible version of Java.
  Use the [Gradle/Java compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html)
  to learn which is the compatible version range.
  The Version of Gradle used in this experiment can be found in the `gradle-wrapper.properties` file
  located in the `gradle/wrapper` folder.
2. Install the version of Python indicated in `.python-version` (or use `pyenv`).
3. Launch either:
    - `./gradlew runAllBatch` on Linux, MacOS, or Windows if a bash-compatible shell is available;
    - `gradlew.bat runAllBatch` on Windows cmd or Powershell;
4. Once the experiment is finished, the results will be available in the `data` folder. Then run:
    - `python -m venv venv`
    - `source venv/bin/activate`
    - `pip install --upgrade pip`
    - `pip install -r requirements.txt`
    - `python process.py`
5. The charts will be available in the `charts` folder.

## Inspect a single experiment

Follow the instructions for reproducing the entire experiment natively, but instead of running `runAllBatch`,
run `runEXPERIMENTGraphics`, replacing `EXPERIMENT` with the name of the experiment you want to run
(namely, with the name of the YAML simulation file, that can be found under `src/main/yaml` folder).

If in doubt, run `./gradlew tasks` to see the list of available tasks.

To make changes to existing experiments and explore/reuse,
we recommend to use the IntelliJ Idea IDE.
Opening the project in IntelliJ Idea will automatically import the project, download the dependencies,
and allow for a smooth development experience.

## Regenerate the charts

We keep a copy of the data in this repository,
so that the charts can be regenerated without having to run the experiment again.
To regenerate the charts, run `docker compose run --no-deps charts`.
Alternatively, follow the steps or the "reproduce natively" section,
starting after the part describing how to re-launch the simulations.
