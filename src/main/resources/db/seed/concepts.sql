-- Generated from frontend/src/main/data/conceptGraph.ts, conceptContent.ts,
-- and conceptGraphPositions.ts. All rows use course_id = 1.

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Basic 
 Data Types', 'Data is any piece of information that your program stores or works with. In Python, the basic data types are integers, floats, strings, and booleans. Data types help Python store and use information correctly.', '7
8.12
"I love learning Python"
False', '#c99ffe', 1265, 1350, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Numeric (integers, floats)', 'Numeric data types are used to store numbers in Python. Integers are whole numbers. Floats, or floating point numbers, are numbers with a decimal point. Both can be positive or negative.', '29
-6.88888', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Basic 
 Data Types'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Strings', 'A string is how Python stores text. A string can be a single character, an entire paragraph, or even an empty space. Strings are created by wrapping text in matching single or double quotation marks. Strings cannot be changed after they are created, but you can create new strings by performing operations on existing strings.', '# Empty string
''''
''This is a string.''
"This is also a string."', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Basic 
 Data Types'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Booleans', 'A boolean data type can only be one of two values: True or False. The value must be capitalized with no quotation marks around it.', 'True
False', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Basic 
 Data Types'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Data 
 Representation', 'Data representation is the way computers store and display information using different numbering systems. These numbering systems are called bases. Since computers work in binary internally, other systems like hexadecimal and decimal exist to represent the same values in more compact or human-readable forms. The same value can be represented in different bases - even though the symbols look different, the value stays the same across each base. A Python "literal" is a value written directly in the code, which can be in decimal, binary, or hexadecimal depending on the prefix.', '# Decimal
10

# Binary
0b1010

# Hex
0xA', '#feaef2', 1730, 1000, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Binary', 'Binary is the most fundamental language a computer speaks, using only two digits: 0 and 1. Every piece of data on a computer is stored as a series of 1s and 0s. Each position in a binary number represents a power of 2, so binary is a base-2 numbering system. In Python, a binary number is written using the prefix 0b.', '# The number 10 in binary:
0b1010

# The number 50 in binary:
0b110010', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Data 
 Representation'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Hex', 'Hexadecimal is a base-16 numbering system that uses 16 symbols — digits 0-9 and letters A through F — to represent values more compactly than binary. Each hex digit represents 4 binary digits. In Python, a hex number is written using the prefix 0x.', '# The number 10 in hex:
0xA

# The number 50 in hex:
0x32', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Data 
 Representation'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Decimal', 'Decimal is the base-10 numbering system that humans use in everyday life. It uses the digits 0-9, and each position represents a power of 10. In Python, numbers written without a prefix are treated as decimal by default.', '# A regular decimal number with no prefix
42', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Data 
 Representation'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Converting between bases', 'To convert any number from base b to decimal, multiply each digit by b raised to the power of its position, then add everything together. Positions are counted from right to left, starting at 0.

To convert a decimal number to base b, repeatedly divide by b, keeping track of the remainders. The remainders, read bottom to top, give you the result.

Steps:
1. Divide the number by b, note the remainder
2. Divide the quotient by b, note the remainder
3. Repeat until the quotient is 0
4. The remainders, read in reverse order, give the result in base b.', '# Convert binary value 1011 to decimal
(1 * 2**3) + (0 * 2**2) + (1 * 2**1) + (1 * 2**0) = 8 + 0 + 2 + 1 = 11

# Convert hex value 2F to decimal (A=10, B=11,…, F=15)
(2 * 16**1) + (15 * 16**0) = 32 + 15 = 47

# Convert decimal value 11 to binary
# 11 ÷ 2 = 5 with remainder 1 
# 5 ÷ 2 = 2 with remainder 1 
# 2 ÷ 2 = 1 with remainder 0 
# 1 ÷ 2 = 0 remainder 1
# Result: 1011', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Data 
 Representation'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Variables', 'A variable is a named container that stores a piece of data so your program can use it later. A variable is like a labeled box, where the label is the variable''s name, and whatever is inside the box is its value. Variables let you reference, update, and reuse information throughout your program.', 'int_variable = 5
string_variable = "CS 8"
boolean_variable = True', '#feaef2', 1420, 1000, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Variable names', 'A variable name is the label you give a variable so you can refer to it in your code. Names are case-sensitive and can only contain letters, numbers, or underscores. They can''t start with a number or contain spaces, and they can''t be Python keywords like ''if'', ''else'', ''for'', etc. Good names clearly describe what the variable holds.', '# Vague variable name
x = "Alejandro"

# Specific variable name
student_name = "Kyle"', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Variables'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Variable assignment', 'Variable assignment creates a variable and gives it a value using the = sign. You can reassign a variable later to overwrite its old value. A variable''s data type changes based on what value is assigned to it.', '# Create variable1 to store a string
variable1 = "ABC"

# Reassign variable1 to store an integer
variable1 = 10

# Update variable1 based on its current value
variable1 = variable1 + 3', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Variables'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, '+= and -=', '+= and -= are shortcuts for updating a variable based on its current value. "+=" adds the value on the right to the variable on the left and stores the result back in that variable. "-=" subtracts the value on the right from the variable on the left and stores the result back in that variable.', 'x = 10

# Same as x = x + 3
x += 3

# Same as x = x - 5
x -= 5', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Variables'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Arithmetic 
 Operations', 'Arithmetic operations are the basic math actions you can perform on numbers in Python. Just like a calculator, Python can add, subtract, multiply, divide, and raise numbers to a power using simple symbols.', '8 + 3
10 / 2', '#feaef2', 800, 1000, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Simple arithmetic', 'The four basic arithmetic operators in Python are: + (add), - (subtract), * (multiply), and ** (raise to a power).', '5 + 3
5 - 3
5 * 3
5 ** 3', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Arithmetic 
 Operations'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Division', 'Python has two kinds of division. Regular division (/) always returns a decimal result, even if both operands are integers and the result is a whole number. Floor division (//) rounds the result down to the nearest whole number.', '# Evaluates to 1.25
5 / 4

# Evaluates to 1
5 // 4', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Arithmetic 
 Operations'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Modulo', 'The modulo operator (%) returns the remainder left over after dividing two numbers. It is useful for checking things like whether a number is even or odd. You can use modulo with floats, but the result will not always be precise due to rounding errors.', '# Evaluates to 1
10 % 3

# Evaluates to 0
10 % 2

# Evaluates to 0.5
1.5 % 1', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Arithmetic 
 Operations'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Order of operations', 'Python follows the standard PEMDAS order when evaluating expressions: parentheses, exponents, multiplication, division, addition, then subtraction.', '# Evaluates to 14
2 + 3 * 4

# Evaluates to 20
(2 + 3) * 4', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Arithmetic 
 Operations'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'String 
 Operations', 'String operations are actions you can perform on text in Python. Python gives you tools to combine, slice, and format strings any time your program needs to work with or display text.', 'greeting = "Hello" + " World!"
word1 = greeting[0:5]', '#93ebff', 1420, 650, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'String concatenation', 'Concatenation joins two or more strings together into one using the + symbol. The strings are combined exactly as they are, so you must include any spaces yourself.', 'word1 = "Hello"
word2 = "World"
greeting = word1 + " " + word2

# Two ways to append to a string
greeting = greeting + "!"
greeting += "!"', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'String 
 Operations'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'String formatting (f-strings)', 'An f-string lets you embed a variable or expression directly inside a string. Place an f before the opening quote, then put variable names or expressions inside curly braces wherever you want their values to appear.', '# Stores "The sum of 5 and 3 is 8"
result = f"The sum of 5 and 3 is {5 + 3}"', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'String 
 Operations'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'String slicing', 'String slicing lets a program extract a smaller portion of a string by specifying a start and stop index: string_variable[start:stop]. The start index is included in the result, but the stop index is not. Leaving out the start or stop index tells Python to slice from the beginning or all the way to the end. String slicing also accepts an optional third value called the step: string_variable[start:stop:step]. The step controls how many positions Python jumps forward each time it picks a value. The default step is 1, meaning Python picks every character. A step of 2 picks every other character, a step of 3 picks every third character, and so on. A negative step counts backwards through the string.', 'name = "Brennan"

# Extracts Bren
name[0:4]

# Every other character: Benn
numbers[::2]

# Reverses the string
name[::-1]', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'String 
 Operations'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Comments', 'A comment is a note in code that Python ignores when running the program. Comments help readers understand what the code is doing and why. They are created by starting a line with a # symbol.', '# This is a comment: Python will not run this line', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'String 
 Operations'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Boolean 
 Expressions', 'A boolean expression is an expression that evaluates to either True or False. Python uses these to make decisions — for example, checking whether two values are equal or a condition is met.', 'x == y
5 != 3
(a and b) or (x and y)', '#feaef2', 1110, 1000, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Comparison: ==, <, <=, !=', 'Comparison operators compare two values and return True or False. == checks if two values are equal, != checks if they are not equal, and <, >, <=, >= compare which value is larger or smaller. <= means "less than or equal to" and >= means "greater than or equal to".', '# Evaluate to True
5 == 5
5 != 3
5 > 3

# Evaluate to False
5 < 3
5 != 5
5 <= 4', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Boolean 
 Expressions'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Logical: and, or, not', 'Logical operators combine or reverse boolean expressions. "and" returns True only if both sides are True, "or" returns True if at least one side is True, and "not" flips a boolean value from True to False or vice versa.', '# Evaluate to True
True and True
not False
True or False

# Evaluate to False
True and False
not True
False or False', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Boolean 
 Expressions'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Membership: in, not in', 'Membership operators check whether a value exists inside a sequence, like if a character is in a string. "in" returns True if the value is found; "not in" returns True if it is not found.', '# Evaluate to True
''P'' in ''Python''
''z'' not in ''Python''

# Evaluate to False
''x'' in ''Python''
''n'' not in ''Python''', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Boolean 
 Expressions'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Conditional 
 Statements', 'A conditional statement lets your program make decisions by running certain code only when a specific condition is met. In Python, conditionals are built using if, elif, and else combined with boolean expressions. The boolean expression is the "condition" in a conditional statement. ', 'test_grade = 85

if test_grade >= 70:
    class_grade = "Pass"
else:
    class_grade = "Fail"', '#93ebff', 180, 650, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'if', 'An if statement checks a condition. If that condition is True, it runs the indented block of code beneath it. If the condition is False, Python skips that block entirely.', 'test_grade = 85

if test_grade >= 80 and test_grade < 90:
    letter_grade = "B"', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Conditional 
 Statements'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'else', 'An else statement goes after an if statement and runs its block when the if condition is False. It acts as a fallback: "if the condition wasn''t met, do this instead." You always need an if statement before an else statement, but you don''t need an else statement after an if statement.', 'test_grade = 85

if test_grade >= 70:
    class_grade = "Pass"
else:
    class_grade = "Fail"', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Conditional 
 Statements'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'elif', 'An elif ("else if") statement lets you check multiple conditions in order. Python evaluates each from top to bottom and runs the first block whose condition is True, skipping all the rest. You always need an if statement before an elif statement. You don''t need an else statement after an elif statement.', 'if test_grade >= 90:
    class_grade = "A"
elif test_grade >= 80:
    class_grade = "B"
elif test_grade >= 70:
    class_grade = "C"
else:
    class_grade = "Did not pass"', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Conditional 
 Statements'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Functions', 'A function is a reusable block of code that performs a specific task. Instead of writing the same code over and over, you can define it once and call it whenever you need it. Functions help keep your code organized, readable, and easier to manage.', 'def greeting(name):
    hello = "Hello, " + name
    return(hello)', '#93ebff', 1110, 650, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Defining functions', 'Defining a function means creating it and giving it a name. Use the def keyword followed by the function name, parentheses, and a colon. The indented code beneath that line is the block that will run whenever the function is called.', 'def sum_function():
    sum = 3 + 5', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Functions'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Calling functions', 'Calling a function means actually running it. You call a function by writing its name followed by parentheses. The code inside a function  (the block indented under the function definition line) will only run if the function is called, and it can be called as many times as you want.', '# Function definition
def sum_function():
    sum = 3 + 5

# Function call
sum_function()', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Functions'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Function parameters', 'A parameter is a variable listed inside a function''s parentheses when the function is defined. It acts as a placeholder for a value that will be passed in when the function is called. This lets you write one function that can work with different inputs. To create a function with multiple parameters, separate the variables with commas inside the parentheses. A function does not have to have parameters — if there is no variable inside the parentheses in the function, the function has no parameters. When calling a function that has parameters, "pass in" the values by listing them inside the function call parentheses. The values that you pass in to a function call are called inputs. ', '# Parameters are a and b
def sum_function(a, b):
    sum = a + b

# Call with inputs x and y
x = 5
y = 3
sum_function(x, y)', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Functions'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Return statements', 'A return statement sends a value back out of a function so it can be used elsewhere in your program. A return statement is also used to exit a function: any code after the return statement will not run. A return statement starts with the "return" keyword followed by the value being returned (the "return value"). If a function does not have a return statement, it returns None by default. If you assign a variable to a function call (like this: variable_name = sum_function() ), the return value will be stored in the variable. ', 'def sum_function(a, b):
    sum = a + b
    return(sum)

# total stores the return value: 8
total = sum_function(5, 3)', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Functions'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Built-in 
 Functions', 'Built-in functions are ready-made functions that come with Python — you don''t need to define them yourself. They handle common tasks like measuring length, finding min/max values, or converting data types. There are over 60 built-in functions in Python, but the most commonly used ones are print(), input(), len(), min(), max(), int(), str(), float(), and range().', '# Returns 3
len("CS8")

# Returns 2
min(8, 2, 13, 4)', '#fe9a71', 1420, 300, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'len()', 'len() returns the number of items in a sequence — such as the number of characters in a string or the number of values in a list. When calling the len() function, pass in the sequence as input. ', '# Returns 5
len("Hello")

letter_list = [''a'', ''b'', ''c'']

# Returns 3
len(letter_list)', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Built-in 
 Functions'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'min() and max()', 'min() returns the smallest value in a group of numbers; max() returns the largest. You can pass in multiple values separated by commas, or a single list of numbers.', '# Returns 4
min(8, 6, 13, 4)

num_list = [6, 2, 9, 12]

# Returns 12
max(num_list)', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Built-in 
 Functions'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'range()', 'range() returns a list of integers. range() takes up to three input values: range(start, stop, step). At least one input value is required. If you pass in one number as input, it will be the stop value: the default starting value is 0 and the default step is 1. If you pass in two numbers as input, they will be the start and stop values. If you pass in three inputs, the step controls how much the sequence increments by each time. The stop value is never included in the result — the list that range() returns always stops just before it. The step can also be negative, which lets you count backwards.', '# Returns [0, 1, 2, 3]
range(4)

# Returns [3, 4, 5, 6]
range(3, 7)

# Returns [2, 4, 6]
range(0, 8, 2)

# Returns [8, 7, 6, 5]
range(8, 4, -1)', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Built-in 
 Functions'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Type casting', 'Type casting means converting a value from one data type to another. Python has built-in functions for this: int() converts a value to an integer, float() converts to a float, bool() converts to a boolean, and str() converts to a string. All of these functions only work if it is possible to convert the original value to the new data type: anything can be converted to a string, but trying to convert a string with letters in it to an integer or float will cause an error. Converting an integer to a float adds .0 to the end of the integer value, and converting a float to an integer removes anything after the decimal point. ', '# Converts to integer 5
a = 5.2
int(a)

# Converts to float 6.0
b = 6
float(b)

# Converts to string "7"
c = 7
str(c)

# Converts to integer 5
d = "5"
int(d)', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Built-in 
 Functions'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'if __name__ 
 == "__main__":', 'The if __name__ == "__main__" block is the standard way to designate the entry point of a program — the place where execution begins. It also controls which parts of the code only run when the file is executed directly, not when it is imported by another file.', 'if __name__ == "__main__":

    # Function calls go here
    function1()
    function2()', '#fe9a71', 180, 300, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Program structure', 'A well-structured Python program follows a consistent layout: imports at the top, then function definitions, then the if __name__ == "__main__" block at the very bottom. This ensures everything is defined before it is used and makes the program easy to read.', 'def function1():
    # Code

def function2():
    # Code

if __name__ == "__main__":

    function1()
    function2()', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'if __name__ 
 == "__main__":'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Using if __name__ == "__main__"', 'Every Python file has a built-in variable called __name__. When you run a file directly, Python sets __name__ to the string "__main__", so any code inside the if __name__ == "__main__" block runs automatically. Code outside this block — like function definitions — is still accessible to other files that import it, but will not run automatically.', 'def function1():
    # Code

if __name__ == "__main__":

    # function1() runs when this file is executed directly
    function1()', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'if __name__ 
 == "__main__":'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Dictionaries', 'A dictionary is a data type that stores information as pairs of keys and values. Instead of accessing data by position, you look up a value using its unique key — similar to looking up a definition using a word in a real dictionary. The keys in a dictionary are unique, meaning there is only one of each key, but multiple keys can be "mapped" to the same value.', 'student_ages = {"Alejandro": 19, "Kyle": 20, "Nikita": 20}', '#93ebff', 1730, 650, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Creating a dictionary', 'A dictionary is created using curly braces {} with each key-value pair separated by a colon. Multiple pairs are separated by commas. Keys are typically strings or numbers, and values can be any data type.', 'student = {"name": "Abby", "age": 20, "major": "Political Science"}', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Dictionaries'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Accessing a value', 'A value in a dictionary is accessed using its key inside square brackets []. If the key doesn''t exist, Python will return an error. If the key is a string, it must be in quotes.', 'student = {"name": "Abby", "age": 20, "major": "Political Science"}

# Stores the integer 20
Abby_age = student["age"]', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Dictionaries'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Adding/updating a key', 'A new key-value pair is added using the format: dictionary_name["new_key"] = value. If the key already exists in the dictionary, its value will be overwritten.', 'student = {"name": "Abby", "age": 20}

# Adds key "major" with value "Political Science"
student["major"] = "Political Science"', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Dictionaries'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Input 
 & Output', 'Input and output are how a program communicates with the user. Output is information the program displays; input is information it receives from the user. Python provides print() for output and input() for input.', 'print("This sentence will be printed out.")
name = input("What is your name?")', '#2bcd9c', 955, 0, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'input()', 'input() pauses the program and waits for the user to type something and press Enter. Whatever the user types is returned as a string. You can pass a prompt message into input() to display instructions or a question to the user.', 'name = input("What is your name? ")
print("Hello, " + name + "!")

# Type casting to store user input as an integer
age = int(input("How old are you? "))
print(age + 1)', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Input 
 & Output'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'print()', 'print() displays information to the screen. You can pass in strings, variables, or expressions. Multiple values separated by commas will be printed with a space between them. By default, print() adds a new line after each call.', '# Prints "4"
print(2 + 2)

# Prints "My name is Kate"
name = "Kate"
print("My name is ", name)', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Input 
 & Output'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Loops', 'A loop runs the same block of code multiple times without writing it out repeatedly. A program uses loops to repeat an action, work through a sequence of values, or keep doing something until a condition is met. Python has two kinds of loops: for loops and while loops.', 'x = 0
while x < 3:
    x += 1

num_list = [10, 5, 20, 3, 15]
total = 0

for num in num_list:
    total += num', '#fe9a71', 490, 300, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'For loops', 'A for loop repeats a block of code once for each item in a sequence (like a list or a range of numbers). The first line of a for loop has this format: for loop_variable in sequence, with a colon at the end of the line. All the code inside of the for loop is indented. The loop variable takes on each value in the sequence one at a time, and the indented block runs once for each value in the sequence. One run of the indented block inside of a for loop is called a loop iteration. ', 'vowels = "aeiou"
vowel_count = 0

for letter in "Computer":
    if letter in vowels:
        vowel_count += 1', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Loops'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'While loops', 'A while loop repeats a block of code as long as a condition remains True. Once the condition becomes False, the loop stops. Use a while loop when you don''t know in advance how many times you need to repeat something.', 'x = 0
while x < 3:
    x += 1', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Loops'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Break', 'The break keyword immediately stops a loop and exits it, even if there are still items left in the sequence or the condition is still True. It is useful when a program needs to stop looping as soon as a specific condition is met.', 'numbers = [3, 7, 1, 9, 4, 6]
target = 9

for num in numbers:
    if num == target:
        break', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Loops'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Continue', 'continue skips the rest of the current loop iteration and jumps straight to the next one. Unlike break, it doesn''t stop the loop entirely — it just moves on to the next item in the sequence.', 'num_list = [10, -5, 20, -3, 15]
total = 0

# Sums only positive numbers
for num in num_list:
    if num < 0:
        continue
    total += num', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Loops'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Iteration', 'Iteration means going through a sequence one item at a time. Every time a loop moves to the next item, that is one iteration. A data type is "iterable" if it can hold multiple individual values that can be stepped through, like a list or a string.', 'a_count = 0

# Iterates over the string "alphabet"
for letter in "alphabet":
    if letter == ''a'':
        a_count += 1', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Loops'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Accumulator pattern', 'The accumulator pattern starts a variable at an initial value before a loop, then updates it with each iteration to build up a result — like a running total, a count, or a growing string. The variable must be declared before the loop so it doesn''t reset on each iteration.', 'num_list = [10, 5, 20, 3, 15]
total = 0

for num in num_list:
    total += num', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Loops'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Lists', 'A list is a data type that stores multiple values in a specific order. A list can hold any combination of data types, and its values can be changed at any time.', 'numbers = [1, 2, 3, 4, 5]', '#93ebff', 800, 650, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Creating a list', 'A list is created using square brackets [] with each value separated by a comma. A list can hold any data type, including a mix of different types. A list can also be empty.', 'mixed = [1, "hello", True]
empty = []', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Lists'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Accessing a value', 'A value in a list is accessed using the value''s numbered position, or index, inside square brackets. List indexes start at 0, so the first item is at index 0, the second at index 1, and so on. A negative index counts from the end of the list.', 'names = ["Alice", "Bob", "Charlie"]

# Stores "Alice"
first_name = names[0]

# Stores "Bob"
second_name = names[1]

# Stores "Charlie"
last_name = names[-1]', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Lists'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'List slicing', 'List slicing lets a program extract a smaller portion of a list by specifying a start and stop index: list[start:stop]. The start index is included in the result, but the stop index is not. Leaving out the start or stop index tells Python to slice from the beginning or all the way to the end. List slicing also accepts an optional third value called the step: list[start:stop:step]. The step controls how many positions Python jumps forward each time it picks a value. The default step is 1, meaning Python picks every item. A step of 2 picks every other item, a step of 3 picks every third item, and so on. A negative step counts backwards through the list. A string can also be sliced with the same rules. ', 'numbers = [10, 20, 30, 40, 50, 60, 70]

# Extracts [30, 40, 50]
numbers[2:5]

# Every other value: [10, 30, 50, 70]
numbers[::2]

# Reverses the list
numbers[::-1]', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Lists'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Nested 
 Lists', 'A nested list is a list that contains other lists as its values. Nested lists are useful for representing structured data with rows and columns, like a grid or table. Each inner list can be accessed and worked with just like a regular list.', 'grid = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]', '#fe9a71', 1110, 300, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Creating a nested list', 'A nested list is created the same way as a regular list, except some or all of its values are themselves lists. Each inner list is separated by a comma, just like any other value.', '# A list containing three lists
grid = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Nested 
 Lists'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Accessing values', 'A value in a nested list is accessed using two indexes in a row. The first index selects the inner list, and the second selects the value within that inner list.', 'grid = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]

# Access the first inner list: [1, 2, 3]
grid[0]

# Access the second item in the first inner list: 2
grid[0][1]

# Access the last item in the last inner list: 9
grid[2][2]', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Nested 
 Lists'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Nested 
 Loops', 'A nested loop is a loop inside the indented block of another loop. For every single iteration of the outer loop, the inner loop runs all the way through completely. A program uses nested loops any time it needs to work through data that has multiple layers, like a grid or a nested list.', 'nested_list = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
total = 0

for row in nested_list:
    for value in row:
        total += value', '#2bcd9c', 645, 0, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Creating a nested loop', 'A nested loop is created by indenting a second loop inside the block of an outer loop. The inner loop finishes all its iterations before the outer loop moves to its next one.', 'x = 0

while x < 3:
    x += 1
    y = 3
    while y > 0:
        y -= 1', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Nested 
 Loops'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Iterating over a nested list', 'A program uses a nested loop to iterate over a nested list by using the outer loop to go through each inner list, and the inner loop to go through each value within that inner list.', 'nested_list = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
total = 0

for row in nested_list:
    for value in row:
        total += value', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Nested 
 Loops'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Tuples', 'A tuple is a data type that stores multiple values in a specific order, similar to a list. The key difference is that a tuple''s values cannot be changed after it is created. A program uses tuples to store data that should stay fixed.', 'student1 = ("Alejandro", 20, "Data Science")
student2 = ("Christos", 18, "Computer Science")', '#93ebff', 2040, 650, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Creating a tuple', 'A tuple is created using parentheses () with each value separated by a comma. It can hold any mix of data types. To create a tuple with only one value, a trailing comma is required after that value.', 'coordinates = (10, 20)
mixed_tuple = (1, "hello", True)

# A tuple with one value
single = (42,)

# An empty tuple
empty = ()', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Tuples'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Accessing a value', 'A value in a tuple is accessed using its index inside square brackets, just like a list. Tuple indexes start at 0, and negative indexes count from the end.', 'coordinates = (10, 20, 30)

# Access the first value: 10
coordinates[0]

# Access the last value: 30
coordinates[-1]

# Access a range of values: (20, 30)
coordinates[1:]', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Tuples'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Sets', 'A set is a data type that stores multiple unique values with no guaranteed order. A program uses sets when duplicates are not allowed and order is not important. Because sets are unordered, values cannot be accessed by index. You can use the "in" operator to check if a value is in a set.', 'fruits = {"apple", "banana", "cherry"}
numbers = {1, 2, 3}', '#93ebff', 2350, 650, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Creating a set', 'A set is created using curly braces {} with each value separated by a comma. Duplicate values are automatically removed. To create an empty set, use set() — using {} alone creates an empty dictionary instead.', '# Duplicates are removed: numbers stores {1, 2, 3}
numbers = {1, 2, 2, 3, 3, 3}

# An empty set
empty = set()', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Sets'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Adding an element', 'To add an element to a set, use set_variable.add(new_element). If the element already exists in the set, nothing changes — sets only store unique items.', 'fruits = {"apple", "banana"}

# Adds "cherry" to the set
fruits.add("cherry")

# Adding a duplicate has no effect
fruits.add("apple")', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Sets'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Recursion', 'Recursion is a technique where a function calls itself to solve a problem. Instead of using a loop, a recursive function breaks a problem down into a smaller version of the same problem, solving it step by step until it reaches a simple stopping point. For example, getting the nth value in the Fibonacci sequence (where each number is the sum of the two numbers before it: 0, 1, 1, 2, 3, 5, 8…) is naturally recursive because to find fib(n), you need fib(n-1) and fib(n-2) - the same problem, just smaller. The simple stopping point is the beginning of the sequence, which is always 0 and 1. ', 'def fibonacci_recursive(n):
    # Base case
    if n <= 1:
        return n

    # Recursive step
    return fibonacci_recursive(n - 1) + fibonacci_recursive(n - 2)', '#fe9a71', 800, 300, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Base case', 'The base case is the condition that stops a recursive function from calling itself. Every recursive function must have one — without it, the function would call itself forever. The base case handles the simplest version of the problem and returns a result directly.', 'def countdown(n):
    # Base case: stop when n reaches 0
    if n == 0:
        return 0
    return countdown(n - 1)', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Recursion'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'State change', 'State change means each recursive call moves the problem closer to the base case by changing the input in some way. Without state change, the function would never reach the base case and would call itself forever. Typically this means passing in a smaller or simpler value each time.', 'def countdown(n):
    if n == 0:
        return 0
    # State change: n gets smaller with each call
    return countdown(n - 1)', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Recursion'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Recursive step', 'The recursive step is the line where the function calls itself with a changed input. Each recursive step handles one piece of the problem and passes the rest to the next call, until the base case is eventually reached.', 'def factorial(n):
    # Base case: factorial of 0 is 1
    if n == 0:
        return 1
    # Recursive step: multiply n by factorial of n - 1
    return n * factorial(n - 1)', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Recursion'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Files', 'A program can interact with files stored on a computer to save or retrieve information. Unlike variables, file data is stored permanently. Python has built-in tools for opening, reading, and writing files. Working with a file requires knowing its filepath string.', '# Filepath string example
"downloads/CS8_data/data.txt"', '#2bcd9c', 1265, 0, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Opening & closing files', 'Before reading or writing a file, a program must open it using open(), passing in the filename and a mode: "r" for reading or "w" for writing. When done, the file should be closed with .close(). Using the "with" keyword is preferred because it closes the file automatically.', '# Opening and closing manually
file = open("data.txt", "r")
file.close()

# Opening with "with" — closes automatically
with open("data.txt", "r") as file:', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Files'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Reading a file', '.read() returns the entire file as one string. .readlines() returns a list where each item is one line from the file as a separate string.', '# contents stores all of data.txt as one string
with open("data.txt", "r") as file:
    contents = file.read()

# lines_list stores each line as a separate string
with open("data.txt", "r") as file:
    lines_list = file.readlines()', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Files'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Writing to a file', 'A program writes to a file using "w" mode and the .write() method. If the file doesn''t exist, Python creates it automatically. Opening an existing file in "w" mode will erase its previous contents.', 'with open("output.txt", "w") as file:
    file.write("Hello, World!")

# output.txt now contains: Hello, World!', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Files'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Built-in 
 Methods', 'A method is a function that is associated with a specific object. Most Python data types come with their own set of built-in methods to perform specific actions. Some methods return a new value; others modify the original object directly.', '# .add() is a built-in set method
num_set = {1, 2, 3}
num_set.add(4)', '#fe9a71', 1730, 300, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Calling a method', 'A method is called by attaching it to a value or variable using a dot, followed by the method name and parentheses. Some methods take inputs inside the parentheses; some do not.', 'numbers = [3, 1, 4, 1, 5]

# Returns 2
numbers.count(1)', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Built-in 
 Methods'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'List methods', 'Four useful built-in list methods are .append(), .pop(), .sort(), and .reverse(). .append(item) adds an item to the end, .pop(index) removes and returns an item at a given index, .sort() sorts least to greatest, and .reverse() reverses the order.', 'num_list = [4, 5, 6, 8, 2]

# num_list becomes [4, 5, 6, 8, 2, 7]
num_list.append(7)

# num_list becomes [5, 6, 8, 2, 7]
num_list.pop(0)

# num_list becomes [7, 2, 8, 5, 6]
num_list.reverse()

# num_list becomes [2, 5, 6, 7, 8]
num_list.sort()', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Built-in 
 Methods'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'String 
 Methods', 'String methods are built-in methods that perform common operations on strings, called using dot notation on a string value or variable. Because strings are immutable, string methods never modify the original string — they always return a new one.', 'name = "Nikita"
uppercase_name = name.upper()
word_list = "hello world".split()', '#2bcd9c', 1885, 0, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'upper() and lower()', '.upper() returns a new string with all characters converted to uppercase. .lower() returns a new string with all characters converted to lowercase.', 'name = "Nikita"

# result: "NIKITA"
uppercase_name = name.upper()

# result: "nikita"
lowercase_name = name.lower()', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'String 
 Methods'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'replace()', '.replace() returns a new string with every occurrence of a specified value swapped out for a new value. Pass in the value to find and the replacement value as inputs.', 'sentence = "I like cats and cats like me."

# result: "I like dogs and dogs like me."
new_sentence = sentence.replace("cats", "dogs")', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'String 
 Methods'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'split()', '.split() breaks a string into a list of smaller strings, dividing at a specified separator. By default it splits on whitespace. The separator itself is not included in the result.', 'sentence = "hello world"

# result: ["hello", "world"]
word_list = sentence.split()

date = "2024-05-08"

# result: ["2024", "05", "08"]
date_list = date.split("-")', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'String 
 Methods'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Imports & Modules', 'A module is a file containing pre-written Python code that a program can use. Instead of writing everything from scratch, a program can import a module to gain access to functions and tools that are already built.', 'import random

# Returns a random integer between 1 and 10
random_int = random.randint(1, 10)', '#2bcd9c', 1575, 0, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Importing modules', 'A module is imported using the import keyword followed by the module name. Once imported, its functions are accessed using dot notation. You can also import a specific function from a module using the from keyword — that function can then be used without dot notation.', '# Import the entire module
import math

# Import a specific function from a module
from random import randint', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Imports & Modules'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'random', 'The random module provides tools for generating random values. It is commonly used in games, simulations, and programs that need unpredictable behavior. randint() returns a random integer between two values, and choice() picks a random item from a list.', 'import random

# Returns a random integer between 1 and 10
random_int = random.randint(1, 10)

fruits = ["apple", "banana", "cherry"]

# A random item from the fruits list
random_fruit = random.choice(fruits)', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Imports & Modules'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'math', 'The math module provides mathematical functions and constants beyond basic arithmetic. Commonly used functions include sqrt() for square roots, floor() for rounding down, and ceil() for rounding up.', 'import math

# Returns the square root: 4.0
math.sqrt(16)

# Rounds down to the nearest integer: 3
math.floor(3.9)

# Rounds up to the nearest integer: 4
math.ceil(3.1)', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Imports & Modules'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Mutability', 'Mutability describes whether the value of an object can be changed after it is created. Mutable objects can be modified in place. Immutable objects are fixed — any operation that seems to modify one actually creates a brand new object instead.', '# The list is changed directly to [99, 2, 3]
numbers = [1, 2, 3]
numbers[0] = 99

# Strings cannot be changed in place
name = "Alice"
# This would cause an error: name[0] = "a"', '#fe9a71', 2040, 300, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Mutable objects', 'Lists and dictionaries are mutable — a program can modify their contents directly without creating a new object. This means two variables pointing to the same list or dictionary will both reflect any changes made to it.', 'a = [1, 2, 3]

# b points to the same list as a
b = a

# Modifying b in place also changes a
b[0] = 99

# a: [99, 2, 3]
# b: [99, 2, 3]', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Mutability'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Immutable objects', 'Tuples and strings are immutable — their contents cannot be changed after creation. If a program needs a modified version, it must create a brand new object. Trying to change an item in place will cause an error.', '# Trying to change a character directly would cause an error
name = "Alice"
# name[0] = "a"  → Error

# A new string must be created instead
name = "a" + name[1:]', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Mutability'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Testing', 'Testing is the process of checking that a program''s code works correctly. Instead of manually verifying results by eye, a program can include automated tests that verify the output of functions for a given input. Writing tests helps catch mistakes early and makes bugs easier to find.', 'def multiply(a, b):
    return a * b

def test_multiply():
    assert multiply(2, 3) == 6', '#fe9a71', 2350, 300, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'assert statements', 'An assert statement checks whether a condition is True. If it is, nothing happens and the program continues. If it is False, Python raises an AssertionError, signaling that something is not working as expected. Assert statements are the building block of writing tests.', '# This passes: no error
assert 2 + 2 == 4

# This fails and raises an AssertionError
assert 2 + 2 == 5', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Testing'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Writing test functions', 'A test function is a function dedicated to testing one specific function in a program. Test function names typically start with test_. The test function calls the function being tested and uses assert statements to verify that the output is correct.', 'def add(a, b):
    return a + b

def test_add():
    assert add(2, 3) == 5
    assert add(0, 0) == 0
    assert add(-1, 1) == 0', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Testing'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Edge cases', 'An edge case is an unusual or extreme input that might cause a function to behave unexpectedly. Good tests always include edge cases in addition to normal inputs. Common edge cases include empty values, zero, negative numbers, and the smallest or largest possible inputs.', 'def divide(a, b):
    return a / b

def test_divide():
    # Normal case
    assert divide(10, 2) == 5.0

    # Edge case: dividing by a negative number
    assert divide(10, -2) == -5.0

    # Edge case: dividing zero by a number
    assert divide(0, 5) == 0.0', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Testing'));

INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Errors & 
 Debugging', 'An error occurs when Python encounters something in the code that it cannot execute. Debugging is the process of finding and fixing those errors. Understanding the different types of errors and how to read Python''s error messages makes it easier to figure out what went wrong and where.', '# The code below causes an error
sum = x + 

SyntaxError: invalid syntax', '#93ebff', 490, 650, NULL);
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Syntax errors', 'A syntax error occurs when the code breaks the rules of the Python language and Python cannot understand it. Python catches these before the program even runs. Common causes include missing colons, mismatched parentheses, or misspelled keywords. An IndentationError is a specific type of syntax error that happens when the indentation in a program is not consistent.', '# Missing colon after if statement
if x > 5
    x = 10

# Missing closing parenthesis
result = (2 + 3', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Errors & 
 Debugging'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Runtime errors', 'A runtime error occurs while the program is running — the syntax is correct, but something goes wrong during execution. Common examples include accessing an index in a sequence that doesn''t exist or using the wrong data type for an operation. Common runtime error types include IndexError (index is out of bounds), TypeError (incorrect data type), ValueError (correct data type but inappropriate value), and NameError (variable is not defined).', '# Accessing an index that doesn''t exist: IndexError
numbers = [1, 2, 3]
numbers[5]

# Concatenating the wrong data type: TypeError
result = "hello" + 5

# Trying to convert a non-numeric string to an integer: ValueError
num = int("abc")

# Using a variable that hasn''t been defined: NameError
print(x)', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Errors & 
 Debugging'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'try / except', 'A try/except block lets a program handle a runtime error gracefully instead of crashing. Python attempts to run the code in the try block, and if an error occurs, it jumps to the except block and runs that instead.', 'try:
    result = "hello" + 5
except TypeError:
    result = "hello"', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Errors & 
 Debugging'));
INSERT INTO concepts (course_id, label, description, example, color, x, y, parent_id) VALUES (1, 'Reading tracebacks', 'A traceback is the error message Python displays when a runtime error occurs. It shows exactly where the error happened and what type it was. The most important parts are the last line (the error type and description) and the line just above it (the exact code that caused the problem).', '# This code causes a traceback
numbers = [1, 2, 3]
numbers[5]

# Traceback (most recent call last):
#   File "main.py", line 2, in <module>
#     numbers[5]
# IndexError: list index out of range', NULL, NULL, NULL, (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Errors & 
 Debugging'));

-- Practice problem URLs

INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/697821834/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Basic 
 Data Types';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Numeric (integers, floats)' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Basic 
 Data Types'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Strings' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Basic 
 Data Types'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Booleans' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Basic 
 Data Types'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/697992229/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Data 
 Representation';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Binary' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Data 
 Representation'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Hex' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Data 
 Representation'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Decimal' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Data 
 Representation'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Converting between bases' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Data 
 Representation'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/697992232/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Variables';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Variable names' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Variables'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Variable assignment' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Variables'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = '+= and -=' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Variables'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/697992231/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Arithmetic 
 Operations';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Simple arithmetic' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Arithmetic 
 Operations'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Division' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Arithmetic 
 Operations'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Modulo' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Arithmetic 
 Operations'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Order of operations' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Arithmetic 
 Operations'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/697992235/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'String 
 Operations';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'String concatenation' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'String 
 Operations'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'String formatting (f-strings)' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'String 
 Operations'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'String slicing' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'String 
 Operations'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Comments' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'String 
 Operations'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/697992230/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Boolean 
 Expressions';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Comparison: ==, <, <=, !=' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Boolean 
 Expressions'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Logical: and, or, not' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Boolean 
 Expressions'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Membership: in, not in' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Boolean 
 Expressions'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/698000002/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Conditional 
 Statements';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'if' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Conditional 
 Statements'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'else' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Conditional 
 Statements'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'elif' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Conditional 
 Statements'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/698000004/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Functions';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Defining functions' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Functions'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Calling functions' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Functions'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Function parameters' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Functions'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Return statements' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Functions'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/698000012/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Built-in 
 Functions';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'len()' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Built-in 
 Functions'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'min() and max()' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Built-in 
 Functions'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'range()' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Built-in 
 Functions'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Type casting' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Built-in 
 Functions'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/698000008/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'if __name__ 
 == "__main__":';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Program structure' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'if __name__ 
 == "__main__":'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Using if __name__ == "__main__"' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'if __name__ 
 == "__main__":'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/698000003/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Dictionaries';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Creating a dictionary' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Dictionaries'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Accessing a value' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Dictionaries'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Adding/updating a key' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Dictionaries'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/698000014/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Input 
 & Output';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'input()' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Input 
 & Output'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'print()' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Input 
 & Output'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/698000010/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Loops';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'For loops' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Loops'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'While loops' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Loops'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Break' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Loops'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Continue' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Loops'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Iteration' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Loops'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Accumulator pattern' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Loops'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/697992236/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Lists';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Creating a list' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Lists'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Accessing a value' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Lists'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'List slicing' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Lists'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/698000013/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Nested 
 Lists';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Creating a nested list' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Nested 
 Lists'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Accessing values' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Nested 
 Lists'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/698000018/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Nested 
 Loops';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Creating a nested loop' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Nested 
 Loops'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Iterating over a nested list' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Nested 
 Loops'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/697992234/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Tuples';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Creating a tuple' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Tuples'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Accessing a value' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Tuples'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/697992233/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Sets';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Creating a set' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Sets'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Adding an element' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Sets'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/698000011/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Recursion';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Base case' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Recursion'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'State change' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Recursion'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Recursive step' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Recursion'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/698000016/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Files';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Opening & closing files' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Files'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Reading a file' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Files'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Writing to a file' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Files'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/698000007/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Built-in 
 Methods';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Calling a method' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Built-in 
 Methods'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'List methods' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Built-in 
 Methods'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/698000017/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'String 
 Methods';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'upper() and lower()' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'String 
 Methods'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'replace()' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'String 
 Methods'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'split()' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'String 
 Methods'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/698000015/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Imports & Modules';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Importing modules' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Imports & Modules'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'random' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Imports & Modules'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'math' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Imports & Modules'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/698000006/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Mutability';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Mutable objects' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Mutability'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Immutable objects' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Mutability'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/698000009/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Testing';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'assert statements' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Testing'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Writing test functions' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Testing'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Edge cases' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Testing'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/pl/course_instance/213067/instance_question/698000005/' FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Errors & 
 Debugging';
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Syntax errors' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Errors & 
 Debugging'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Runtime errors' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Errors & 
 Debugging'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'try / except' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Errors & 
 Debugging'));
INSERT INTO practice_problems (course_id, concept_id, url) SELECT 1, id, 'https://us.prairielearn.com/' FROM concepts WHERE course_id = 1 AND (label = 'Reading tracebacks' AND parent_id = (SELECT id FROM concepts WHERE course_id = 1 AND parent_id IS NULL AND label = 'Errors & 
 Debugging'));

-- Prerequisite edges between concepts

INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Basic 
 Data Types' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Variables';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Basic 
 Data Types' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Arithmetic 
 Operations';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Basic 
 Data Types' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Data 
 Representation';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Basic 
 Data Types' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Boolean 
 Expressions';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Variables' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Lists';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Variables' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Functions';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Variables' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Dictionaries';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Variables' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Sets';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Variables' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Tuples';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Variables' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'String 
 Operations';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Boolean 
 Expressions' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Conditional 
 Statements';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Boolean 
 Expressions' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Loops';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Variables' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Errors & 
 Debugging';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Arithmetic 
 Operations' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Errors & 
 Debugging';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Arithmetic 
 Operations' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Recursion';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Arithmetic 
 Operations' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Loops';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Arithmetic 
 Operations' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'String 
 Operations';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Conditional 
 Statements' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'if __name__ 
 == "__main__":';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Conditional 
 Statements' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Recursion';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Functions' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Testing';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Functions' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'if __name__ 
 == "__main__":';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Functions' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Built-in 
 Functions';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Functions' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Built-in 
 Methods';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Functions' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Recursion';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Lists' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Nested 
 Lists';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Lists' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Mutability';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Lists' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Loops';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Dictionaries' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Mutability';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Tuples' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Mutability';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Sets' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Mutability';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Loops' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Nested 
 Loops';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Built-in 
 Functions' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Input 
 & Output';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Built-in 
 Functions' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Files';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Built-in 
 Methods' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'String 
 Methods';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Built-in 
 Methods' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Imports & Modules';
INSERT INTO concept_edges (course_id, source_concept_id, target_concept_id) SELECT 1, s.id, t.id FROM concepts s, concepts t WHERE s.course_id = 1 AND s.parent_id IS NULL AND s.label = 'Nested 
 Lists' AND t.course_id = 1 AND t.parent_id IS NULL AND t.label = 'Nested 
 Loops';
