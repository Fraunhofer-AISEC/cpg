class counter:
    pass

def count(c):
  if c.inc() < 5:
    count(c)

class c1(counter):
  total = 0

  def inc(self):
    self.total = self.total + 1
    return self.total

count(c1())
