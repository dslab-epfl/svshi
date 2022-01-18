import sys

print("this a line of print on stdout")
print("this is a second line on stdout")
print("this is a third line on stdout")

print("this a line of print on stderr", file=sys.stderr)
print("this is a second line on stderr", file=sys.stderr)
print("this is a third line on stderr", file=sys.stderr)

exit(0)