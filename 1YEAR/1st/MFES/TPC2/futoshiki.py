from z3 import * # Import SMT Solver
import re


s = Solver() # Create SMT Solver

input_file = open("input1.txt","r")
content = input_file.readlines() # Array containing all lines of file

N = len(content[0].strip().split()) # remove first element of array and store in variable N

#print(N)
#print(content)

puzzle = []
for i in range(N): # iterate over each line
    value = content[i]
    res = value.split(" ")
    t = []
    for elem in res: # for each element in array 
        t.append(int(elem.strip()))
    puzzle.insert(i,t)

for j in range(N):
    content.pop(0) # Remove matrix elements from content
content.pop(0) # Remove line from content



# 5x5 matrix of integer variables
X = [ [ Int("x_%s_%s" % (i+1, j+1)) for j in range(5) ]
      for i in range(5) ]


# each cell contains a value in {1, ..., 5}
cells_c  = [ And(1 <= X[i][j], X[i][j] <= 5)
             for i in range(5) for j in range(5) ]


# each row contains a digit at most once
rows_c   = [ Distinct(X[i]) for i in range(5) ]


# each column contains a digit at most once
cols_c   = [ Distinct([ X[i][j] for i in range(5) ])
             for j in range(5) ]


# Parse all inequality restrictions from input file
inequality = []
for elem in content: # iterate over restrictions 
    aux = elem.strip().split(" ")
    aux[0] = int(aux[0])
    aux[2] = int(aux[2])

    if (aux[1].find("<") != -1):
        ineq =  X[aux[0]//N][aux[0]%N] < X[aux[2]//N][aux[2]%N]
    elif (aux[1].find(">") != -1):
        ineq =  X[aux[0]//N][aux[0]%N] > X[aux[2]//N][aux[2]%N]

    inequality.append(ineq)

futoshiki_c = cells_c + rows_c + cols_c + inequality  # All restrictions 

instance_c = [ If(puzzle[i][j] == 0,
                  True,
                  X[i][j] == puzzle[i][j])
               for i in range(5) for j in range(5) ]  


s = Solver()
s.add(futoshiki_c + instance_c)
if s.check() == sat:
    m = s.model()
    r = [ [ m.evaluate(X[i][j]) for j in range(5) ]
          for i in range(5) ]
    print_matrix(r)
else:
    print ("failed to solve")
