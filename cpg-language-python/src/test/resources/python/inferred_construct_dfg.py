from sqlalchemy.orm.declarative_base import Base

class A(Base):
    __tablename__ = 'a'
    b = Column(Integer, nullable= False)

def foo():
    a_var = 1
    obj = A(b=a_var)
    return obj

def bar():
    some_other_var = 42
    obj = A(b=some_other_var)
    return obj