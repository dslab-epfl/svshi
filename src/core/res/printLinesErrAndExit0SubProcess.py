import sys

print("this a line of print on stderr", file=sys.stderr)
print("this is a second line", file=sys.stderr)
print("this is a third line", file=sys.stderr)

exit(0)