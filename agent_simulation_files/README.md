Agent files are text files that contain the positions of agents throughout a _"scene"_ as well
as some meta info about the agents like their sensing range.

The top of the agent file tells the simulation the max bounds of the scene.
This is followed by an initialization block that contains the number of agents in the scene and the sensing range of the agents.
The rest of the file contains the positions of the agents in the scene per round.
See `formatGuide.txt` for more details.

You can see how we make custom agent files in the `/customAgentFileGeneratorPython/` folder.

Other deprecated, legacy information also exists and is unfortunately necesarry, such as:
- total scene time
- phone positions of agents