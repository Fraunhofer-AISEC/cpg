class Employee:
    def __init__(self, name):
        self.name = name

class Team:
    """
    Represents a team of employees. The list of team members is only visible
    to the team manager and the team members themselves.
    """
    def __init__(self, name, manager):
        self.name = name
        self.__members = []
        self.manager = manager

    def add_member(self, member: Employee):
        self.__members.append(member)

    def list_members(self, whoami: Employee):
        if whoami == self.manager:
            return self.__members
        elif whoami in self.__members:
            return self.__members
        else:
            return "You are not authorized to view the members of this team."

    def list_member2(self):
        return self.__members
