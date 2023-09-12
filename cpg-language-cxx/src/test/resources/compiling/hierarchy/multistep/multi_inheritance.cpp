class Root {};
class Level0: Root {};
class Level0B {};
class Level1: Level0 {};
class Level1B: Level0, Level0B {};
class Level1C: Level0B, Root {};
class Level2: Level1 {};
class Level2B: Level1B, Level1C {};
class Unrelated {};
/*
Type hierarchy:
          Root------------
           |             |
         Level0  Level0B |
          / \     /  \   |
     Level1 Level1B  Level1C
       |       \       /
     Level2     Level2B
*/