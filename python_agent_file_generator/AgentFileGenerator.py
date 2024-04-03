import numpy as np
import matplotlib.pyplot as plt
from matplotlib.patches import Patch
import subprocess

# all unites in feet
CIRCULAR_TABLE_DIAMETER = 5
DEGREES_PER_CIRCLE = 360
RECTANGULAR_TABLE_WIDTH = 6
RECTANGULAR_TABLE_LENGTH = 2.5

PEOPLE_AT_CIRCULAR_TABLE = 8
PEOPLE_AT_RECTANGULAR_TABLE_WIDTH_SIDE = 3
PEOPLE_AT_RECTANGULAR_TABLE_LENGTH_SIDE = 1

DISTANCE_BETWEEN_TABLES_FOR_CHAIRS = 4

JAR_LOCATION = "~/Research/CoCoT/Simulation/Production/Covid-Graph-ContactTracing.jar"


round_table_experiment = True
cafeteria_experiment = True
conference_experiment = True
sparse_cafeteria_experiment = True
sparse_round_table_experiment = True
sparse_conference_experiment = True


def run_test(agent_file):
    # WIP
    cmd = ["java", "-jar", JAR_LOCATION,
           "dataLocation=~/Research/CoCoT/Simulation/Production/CDFs/",
           "numMalicious=0",
           "sybilMultiplier=1",
           "weights=fractional",
           "alpha=3.0",
           "weightBias=8.9",
           "weightDiscrepancy=0.43",
           "weightSeparation=.14",
           "delta=absolute",
           "maliciousStrategy=nDCFBreak",
           "threshStressMin=-1",
           "threshStressMax=-1",
           "rngSeed=80085",
           "distanceMeasure=ML",
           "fullOutput={}".format(agent_file.split(".")[0] + ".csv"),
           "setting=~/Research/CoCoT/Simulation/pythonScripts/custom_agent_files/" + agent_file]
    subprocess.run(cmd)



def setup_figure(title=""):
    # plt.figure(figsize=(10, 10))
    plt.figure()
    plt.axis('equal')
    plt.grid(linewidth=0.4, linestyle='--')
    plt.gca().set_aspect('equal')
    plt.xlabel("x (ft)")
    plt.ylabel("y (ft)")
    if title != "":
        plt.title(title)

    legend_element = Patch(color='green', label='Person')
    plt.gca().legend(handles=[legend_element])


def stamp_circle_table(center_x, center_y):
    # stamp the table
    ax = plt.gca()
    ax.add_patch(plt.Circle((center_x, center_y), CIRCULAR_TABLE_DIAMETER / 2, color='black', fill=False))

    # stamp the people
    people = []
    for i in range(PEOPLE_AT_CIRCULAR_TABLE):
        angle = i * DEGREES_PER_CIRCLE / PEOPLE_AT_CIRCULAR_TABLE
        x = center_x + np.cos(np.deg2rad(angle)) * CIRCULAR_TABLE_DIAMETER / 2
        y = center_y + np.sin(np.deg2rad(angle)) * CIRCULAR_TABLE_DIAMETER / 2
        plt.scatter(x, y, s=50, c='green', marker='o')
        people.append([x, y])
    return people


def stamp_square_table(corner_x, corner_y,
                       rotation=0,
                       people_on_bottom=True,
                       people_on_left=True,
                       people_on_top=True,
                       people_on_right=True):
    # stamp the table
    ax = plt.gca()
    ax.add_patch(plt.Rectangle((corner_x, corner_y), RECTANGULAR_TABLE_WIDTH, RECTANGULAR_TABLE_LENGTH,
                               color='black', fill=False, angle=rotation))

    # stamp the people
    people = []
    if people_on_bottom:
        for i in range(PEOPLE_AT_RECTANGULAR_TABLE_WIDTH_SIDE):
            x = corner_x + (i + 1) * RECTANGULAR_TABLE_WIDTH / (PEOPLE_AT_RECTANGULAR_TABLE_WIDTH_SIDE + 1) * np.cos(
                np.deg2rad(rotation))
            y = corner_y + (i + 1) * RECTANGULAR_TABLE_WIDTH / (PEOPLE_AT_RECTANGULAR_TABLE_WIDTH_SIDE + 1) * np.sin(
                np.deg2rad(rotation))
            plt.scatter(x, y, s=50, c='green', marker='o')
            people.append([x, y])

    if people_on_left:
        for i in range(PEOPLE_AT_RECTANGULAR_TABLE_LENGTH_SIDE):
            x = corner_x + (i + 1) * RECTANGULAR_TABLE_LENGTH / (PEOPLE_AT_RECTANGULAR_TABLE_LENGTH_SIDE + 1) * np.cos(
                np.deg2rad(rotation + 90))
            y = corner_y + (i + 1) * RECTANGULAR_TABLE_LENGTH / (PEOPLE_AT_RECTANGULAR_TABLE_LENGTH_SIDE + 1) * np.sin(
                np.deg2rad(rotation + 90))
            plt.scatter(x, y, s=50, c='green', marker='o')
            people.append([x, y])

    if people_on_top:
        for i in range(PEOPLE_AT_RECTANGULAR_TABLE_WIDTH_SIDE):
            x = corner_x + (i + 1) * RECTANGULAR_TABLE_WIDTH / (PEOPLE_AT_RECTANGULAR_TABLE_WIDTH_SIDE + 1) \
                * np.cos(np.deg2rad(rotation)) \
                + RECTANGULAR_TABLE_LENGTH * np.cos(np.deg2rad(rotation + 90))
            y = corner_y + (i + 1) * RECTANGULAR_TABLE_WIDTH / (PEOPLE_AT_RECTANGULAR_TABLE_WIDTH_SIDE + 1) \
                * np.sin(np.deg2rad(rotation)) \
                + RECTANGULAR_TABLE_LENGTH * np.sin(np.deg2rad(rotation + 90))
            plt.scatter(x, y, s=50, c='green', marker='o')
            people.append([x, y])

    if people_on_right:
        for i in range(PEOPLE_AT_RECTANGULAR_TABLE_LENGTH_SIDE):
            x = corner_x + (i + 1) * RECTANGULAR_TABLE_LENGTH / (PEOPLE_AT_RECTANGULAR_TABLE_LENGTH_SIDE + 1) \
                * np.cos(np.deg2rad(rotation + 90)) \
                + RECTANGULAR_TABLE_WIDTH * np.cos(np.deg2rad(rotation))
            y = corner_y + (i + 1) * RECTANGULAR_TABLE_LENGTH / (PEOPLE_AT_RECTANGULAR_TABLE_LENGTH_SIDE + 1) \
                * np.sin(np.deg2rad(rotation + 90)) \
                + RECTANGULAR_TABLE_WIDTH * np.sin(np.deg2rad(rotation))
            plt.scatter(x, y, s=50, c='green', marker='o')
            people.append([x, y])

    return people


def print_file(people, OutputFile):
    people = np.array(people)
    mins = people.min(axis=0)
    people = people - mins
    maxs = people.max(axis=0)

    f = open(OutputFile + ".txt", "w")
    f.write("1 minutes\n")
    f.write("{} {} area\n".format(maxs[0], maxs[1]))
    f.write("\n")
    f.write("initialize:\n")
    f.write("\n")

    index = 0
    for person in people:
        f.write("agent {}:\n".format(index))
        f.write("16 range\n")
        f.write("INHAND phone\n")
        f.write("\n")
        index += 1

    f.write("round 0:\n")
    index = 0
    for person in people:
        f.write("{} {:.3f} {:.3f}\n".format(index, person[0], person[1]))
        index += 1
    f.close()
    plt.savefig(OutputFile + ".svg")


if round_table_experiment:
    OutputFile = "roundTables"
    table_grid = [3, 2]
    people = []
    setup_figure("round tables")
    for table_x in range(table_grid[0]):
        for table_y in range(table_grid[1]):
            center_x = table_x * (CIRCULAR_TABLE_DIAMETER + DISTANCE_BETWEEN_TABLES_FOR_CHAIRS)
            center_y = table_y * (CIRCULAR_TABLE_DIAMETER + DISTANCE_BETWEEN_TABLES_FOR_CHAIRS)
            people = people + stamp_circle_table(center_x, center_y)

    #plt.show()
    print_file(people, OutputFile)

if cafeteria_experiment:
    OutputFile = "cafeteria"
    table_grid = [3, 2]
    people = []
    setup_figure("cafeteria")
    for table_x in range(table_grid[0]):
        for table_y in range(table_grid[1]):
            center_x = table_x * (RECTANGULAR_TABLE_WIDTH)
            center_y = table_y * (RECTANGULAR_TABLE_LENGTH + DISTANCE_BETWEEN_TABLES_FOR_CHAIRS)
            people = people + stamp_square_table(center_x, center_y,
                                                 people_on_left=False,
                                                 people_on_right=False, )

    #plt.show()
    print_file(people, OutputFile)

if conference_experiment:
    OutputFile = "conference"
    table_grid = [4]
    people = []
    setup_figure("conference")
    for table_x in range(table_grid[0]):
        table_y = 0
        center_x = table_x * (RECTANGULAR_TABLE_WIDTH)
        center_y = table_y * (RECTANGULAR_TABLE_LENGTH + DISTANCE_BETWEEN_TABLES_FOR_CHAIRS)
        people = people + stamp_square_table(center_x, center_y,
                                             people_on_left=table_x == 0,
                                             people_on_right=table_x == max(table_grid) - 1, )

    #plt.show()
    print_file(people, OutputFile)

if sparse_cafeteria_experiment:
    OutputFile = "sparseCafeteria"
    table_grid = [(0, 0), (2, 1)]
    people = []
    setup_figure("sparse cafeteria")
    for table_loc in table_grid:
        table_x = table_loc[0]
        table_y = table_loc[1]
        center_x = table_x * (RECTANGULAR_TABLE_WIDTH)
        center_y = table_y * (RECTANGULAR_TABLE_LENGTH + DISTANCE_BETWEEN_TABLES_FOR_CHAIRS)
        people = people + stamp_square_table(center_x, center_y,
                                             people_on_left=table_x == table_grid[0][0],
                                             people_on_right=table_x == max(table_grid[1]) - 1, )

    #plt.show()
    print_file(people, OutputFile)

if sparse_round_table_experiment:
    OutputFile = "sparseRoundTables"
    table_grid = [(0, 0), (2, 1)]
    people = []
    setup_figure("sparse round tables")
    for table_loc in table_grid:
        table_x = table_loc[0]
        table_y = table_loc[1]
        center_x = table_x * (CIRCULAR_TABLE_DIAMETER + DISTANCE_BETWEEN_TABLES_FOR_CHAIRS)
        center_y = table_y * (CIRCULAR_TABLE_DIAMETER + DISTANCE_BETWEEN_TABLES_FOR_CHAIRS)
        people = people + stamp_circle_table(center_x, center_y)

    #plt.show()
    print_file(people, OutputFile)

if sparse_conference_experiment:
    OutputFile = "sparse_conference"
    table_grid = [4]
    people = []
    setup_figure("sparse conference")
    for table_x in range(table_grid[0]):
        table_y = 0
        center_x = table_x * (RECTANGULAR_TABLE_WIDTH)
        center_y = table_y * (RECTANGULAR_TABLE_LENGTH + DISTANCE_BETWEEN_TABLES_FOR_CHAIRS)
        people = people + stamp_square_table(center_x, center_y,
                                             people_on_left=table_x == 0,
                                             people_on_right=table_x == max(table_grid) - 1, )
    missing_people = people[1::2]
    people = people[::2]
    for person in people:
        x = person[0]
        y = person[1]
        plt.scatter(x, y, s=50, c='green', marker='o')
    for person in missing_people:
        x = person[0]
        y = person[1]
        plt.scatter(x, y, s=50, c='yellow', marker='o')
    chair_element = Patch(color='yellow', label='Empty Chair')
    person_element = Patch(color='green', label='Person')
    plt.gca().legend(handles=[chair_element, person_element])
    #plt.show()
    print_file(people, OutputFile)
