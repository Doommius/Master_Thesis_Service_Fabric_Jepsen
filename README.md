# thesis
Masters thesis by Mark Jervelund on Jepsen test



Overleaf Doc: https://www.overleaf.com/2773368222ddtjxhhzckwf



Note about clojure generator. 
Clojure

Build list of operations contains
Id - > int
Operation > str, enum or int. ? Not sure yet. String will probably be easiest but int/enum is quicker.. 
Values -> list [] (key, value, expected) maybe have it as a dict? 

Operation:
   Id: int
   Type: str
   Values: 
            key: string
            Value: int
            Expected: int
   Return: future return value that can be used and compares to history. 

Then simply build a list of these. And convert it to json and send it off to the backend? 
