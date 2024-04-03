# CoCoT: Collaborative Contact Tracing Simulation Tool


This tool was used to help test the algorithms in our published paper "CoCoT: Collaborative Contact Tracing" ([codaspy version]() <-please cite this version with included [bibtex](citation.bibtex), [KiltHub version]()). 
`CoCoTsimulator` (CoCoT) is a java tool that simulates wireless contact tracing protocols between phones to experiment with improving their accuracy, 
even in the face of adversaries.
The simulator places people (agents) in a 2d environment, simulates BLE-based distance estimates between them, and then evaluates how different algorithms improve distance-estimate accuracy.
People can be placed in realistic, repeatable ways using AgentFiles, which tell CoCoT where people would be positioned in the 2D area over time.

## Quickstart guide

* I run CoCoT using JDK 19 or later, but CoCoT seems to work on as little as openJDK 17.
* I build CoCoT with Gradle 8.3.
* Code has been tested on Ubuntu 22.04 and MacOS 14.2. 

CoCoT can use agent simulation files to manually position people for repeatable, realistic results.
See the `agent_simulation_files/formatGuide.txt` for details. 


### Compiling and running
I provide a [gradle](https://gradle.org/) file for automating running and compiling. If you want to just run the file, you can use 

>```gradle run (--args="...")```

for example:

> `gradle run --args="setting=agent_simulation_files/SALSAscene.txt fullOutput=salsaOutput.csv"`

This is the easy, automatic way of running CoCoT.
When you run this command, you won't see any command line output, but you should see files being generated. 
In this case, the SALSA dataset takes about 20 seconds to run on my machine; the smaller, synthetic datasets like cafeteria should only take a second or two. 
After that, you'll get a few output files: a large output, the same one as in the sample datasets directory, as well as an abbreviated, summary result. 

If you want a portable jar file, you can use:

>```gradle assemble```

Which will build and package the jar with dependencies into the `build/distributions/` folder as a tar or zip file. 
Later you can untar/unzip it and run the executable with compiled gradle script. 
Note you need to link the compiled jar files. Gradle does this automatically in the run script but see below to avoid gradle. 

If you don't care about compiling it *and* don't want to install gradle, a precompiled version is packaged in this repo.
You can run it by doing:

>```java -classpath precompiled/CoCoT.jar:precompiled/commons-math3-3.3.jar Main (<args>)```

#### Paper Experiments
The results from our experiments can be achieved running CoCoT on SALSA as well as the 6 synthetic experiments. 
The default parameters for CoCoT are the correct arguments to run the SpringMass algorithm.
This is with the one exception that we ran our algorithm with several seeds and averaged the results, for example you could run:
> `gradle run --args="setting=agent_simulation_files/SALSAscene.txt fullOutput=salsaOutput.csv writeOutputHeader=true rngSeed=1"`
> 
> `gradle run --args="setting=agent_simulation_files/SALSAscene.txt fullOutput=salsaOutput.csv writeOutputHeader=false rngSeed=2"`
> 
> `gradle run --args="setting=agent_simulation_files/SALSAscene.txt fullOutput=salsaOutput.csv writeOutputHeader=false rngSeed=3"`
>
> ...

CoCoT will concatenate all the outputs. 
Then you can run whatever statistical tool you like; I find Python+Pandas works quite well with our output.

### Agent files

Agent files are used to place agents in a specific location for repeatable results.
This repo comes packaged with a few useful ones in the `agent_simulation_files/` folder as well as how we generated our 
custom ones in the `python_agent_file_generator/` folder.

More details can be found in the [`agent_simulation_files/README.md`](agent_simulation_files/).

## Project structure

- `src` - Java source directory.
- `precompiled` - Precompiled jar files for running the tool.
- `agent_simulation_files` - contains the agent files used to place agents in specific locations.
- `CDFs` - Cumulative Distributions Functions for distance estimates, this tells the tool how to make realistic distance estimates. Needs to be in the folder you run the tool in.
- `python_agent_file_generator` - A sample python file for making nice agent files. In it's own directory because it makes output files. 
- `sample_datasets` - precompiled datasets resulting from the tool.
- `gradle` - Gradle files (you can ignore this directory)


## Program Arguments

When I wrote this I was stubborn and didn't use a real argument parser, as such arguments are passed as `key=value` pairs.
No spaces between the key, `=`, and value are allowed.

Bellow is a full list of the available arguments:


### Agent placements

`setting`- The agent file to use. 
Not including this argument will default to randomly placing agents in a box using the 
`bounds`, `numberOfRounds`, and `population` arguments. Default=` ` (no setting)

`bounds` - Bounds in feet for the agents to be placed in. Superceded by `setting`. Default=`500`.

`numberOfRounds` - Number of rounds to simulate agents moving. Superceded by `setting`. Default=`20`.

`population` - Number of agents to simulate. Superceded by `setting`. Default=`1000`.

### Algorithm values

Input values for SpringMass algorithm, see paper for details. 

`alpha` - Default=`2.66`

`weightBias` - Default=`8.95`

`weightDiscrepancy` - Default=`0.42`

`weightSeparation` - Default=0.26`

`weights` - Default=`absolute`

`threshStressMin` - Default=`-1.0` (off)

`threshStressMax` - Default=`-1.0` (off)

`delta` - Options={`absolute`,`relative`}, 
Default=`"absolute"`

### Attacker capabilities

By default, attacker capabilities are turned off, i.e. no attacker present for others to create datasets.

`numMalicious` - The number of attackers present. Attackers are chosen randomly out of the crowd. Default=`0`

`maliciousStrategy` - The strategy the attacker uses. See paper for details. 
Options={`close`,`far`,`nDCFBreak`},
Default=`close`

`sybilMultiplier` - The number of sybil identities each attacker assumes. Should be at least 1 for attackers to work. 
Default=`1`

### Which algorithms to run

You can run multiple algorithms in the same simulation to directly compare the results of different algorithms. 
See paper for details on specific algorithms. 
All values are boolean, defaulting to `false` except for `weightedSprings`.

Example: `averageOut=true stressMajorization=true` will run both the averageOut algorithm and the graphDrawing algorithm.

Options={
* `averageOut` - averages out distances estimates between neighbors,
* `stressMajorization` - aka graphDrawing; produces an internal topology of nearby neighbors to optimize over,
* `weightedSprings` - aka springMass; like graph drawing but uses heuristics to determine which estimates are better than others,
* `cliqueMDS` - (deprecated) like graph drawing but emphasizes distance estimates between cliques, ignores neighbors who don't see each other,
* `stressMajDropNeighbor` - (deprecated) like graph drawing but estimates which neighbor was the least beneficial from the estimates and ignores them,
* `stressMajDropLink` - (deprecated) like `stressMajDropNeighbor` but estiamtes which links were the least beneficial,

}

Note that names don't match up from the paper, the options, and the outputCSV. Sorry. 

### Output options

`writeOutputHeader` - Writes the header for the csv file. Default=`false`.

`output` - Location to write the abbreviated output. Default=`outputResults.csv`.

`fullOutput` - Location to write the full output. This will largely be more useful than the regular output. Default=` ` (none, ignored).

`dataLocation` - Where the CDFs for distribution-to-distance-estimates are held. Default=`CDFs/`.


### Miscelaneous

`symmetric` - Determines whether or not agents can talk to other agents that see them, but not vice versa.
E.g. if $p_1$ has a range of 20 and $p_2$ is 12 feet away with a range of 10, $p_1$ can see $p_2$ but $p_2$ can't see $p_1$.
This is best kept to `true` because algorithms tend to do worse with it set to `false` and the situation is more realistic.
Default=`true`.


`visualsON` - Mostly deprecated. 
Provides a janky (😄) visual renderer for the location of agents as well as the relative performances of algorithms in nDCF-like accuracy.
Useful for debugging agent files. 
Default=`false`.

`rngSeed` - seed for the random number generator. Default=`1337`.

`distanceMeasure` - mostly Deprecated. It's possible to use pure random distances, RSSI-based measurements, or precomputed distances (a large list of distances to sample). 
These were mostly for testing purposes and now the inverseCDFs are by far the best option.
Left in code for posterity and in case someone wants to use them eventually.
Options={`RSSI`,`Rand`,`ML`,`DATASET`}, Default=`ML`.

`PrecompiledDistances` - mostly deprecated, don't use.


## Extending the tool

All the code is available in the `src/` folder, feel free to extend it.
Much of the code that was tested for debugging or potential other features still exists in skeleton form. 
Hopefully it makes it easier to understand. 

---
All work in this repo is licensed under the Apache 2.0 license.

This is research grade code and is not guaranteed to be bug free.
This repo is not actively maintained, issues and PRs *might* be addressed. 
Feel free to reach out to me if you have questions. 
