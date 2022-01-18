import time
import sys
with open("../input/ok.txt", "w"):
  print("this a line of text printed by the runtime module on stdout")
  print("this a line of text printed by the runtime module on stderr", file=sys.stderr)
  time.sleep(3)
  print("this is a line of text printed by runtime module on stdout after 3 sec")