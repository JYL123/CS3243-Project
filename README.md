# Learn to Play Tetris with Big Data

![Alt text](https://github.com/JYL123/Tetris/blob/master/Report/tetris_pic.png)

## Goal of this project
The goal of this project is to implement an agent that plays [Tetris](https://tetris.com/) well with big data.

## Goal of the game
The goal of the game is to clear as many rows as possible. 

## Features
These are the features identified for a single game state:
1. ***Height difference***: the height difference between the highest column and the lowest column in the gameboard
2. ***Number of rows cleared***: number of rows whoes cells have been completely filled (and thus removed from the grid)
3. ***Row Transition***: a row transition refers to the occurrence of a filled cell next to a hole in the same row
4. ***Column Transition***: a column transition refers to the occurrence of a filled cell that is next to a hole in the smae column
5. ***Number of holes***: number of empty cells in the grid such that each cell has at least one filled cell on top of it in the same column
6. ***Number of wells***: wells refer to groups of holes whose left and right cells have been completely filled


## Implementation of agent
To implement our agent, we majorly utilized the following 2 algorithms:
* [Genetic algorithm](https://uk.mathworks.com/help/gads/what-is-the-genetic-algorithm.html): this is for selecting good sets of weights for each `features`, as shown above. The "good" set of weights is supposed to maximise the sum of products of wieghts and their corresponding feature values, which we call, `utility function`. There are other small details in our implementation, such as `swap mutation`, and `tournament selection`, etc. which please refer to [our report](https://github.com/JYL123/Tetris/blob/master/Report/Report.pdf).

* [Expectimax](https://inst.eecs.berkeley.edu/~cs188/fa10/slides/FA10%20cs188%20lecture%208%20--%20utilities%20(6PP).pdf): this is for predicting 1 step ahead the next best allocation of the current teromino piece by evaluating all possible moves of the current piece using our [utility function](https://github.com/JYL123/Tetris/blob/master/README#L23).

* Big data: we use [Java 8 parallel programming](https://docs.oracle.com/javase/tutorial/collections/streams/parallelism.html) to scale up our search for a good set of weights. This implmentation shows obvious improvement, as shown below, in the game play.

## Performance
* Parallel stream vs. Sequential stream

![Alt text](https://github.com/JYL123/Tetris/blob/master/Report/para_seq.png)

* Performance on difficult Tetris versions:

![Alt text](https://github.com/JYL123/Tetris/blob/master/Report/tests.png)



