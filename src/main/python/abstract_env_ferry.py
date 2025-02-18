import numpy as np
import random
from enum import Enum
import logging

class Action(Enum):
    LEFT = 0 ; DOWN = 1; RIGHT = 2; UP = 3 # ; NOTHING = 4

class Tile(Enum):
    _FLOOR=0; ROBOT=1; TARGET=2; 
    def __str__(self):
        return self.name[:1]

class FerryAgent:

    def __init__(self, size=9, static_entetys=1, num_ferrys=2, max_size=2, logging_level=logging.INFO, colision=[True,True,True]):
        """
        Init environment
        param:
            size = 9
                default size for environment automaticly scales with number of ferrys and max size
            static entetys = 1
                site of bottelneck scales automaticly with max size
            num_ferrys = 2
                number of ferry agents that are created
            max_size = 2
                maximum size of agents
            colision = [True, True, True]
                out of bound,  other target positions, wall
            
        """
        # logger = logging.getLogger(__name__)
        # logging.basicConfig(filename='logs/Abstract_ferry.log', encoding='utf-8', level=logging_level)
        # logging.info("FERRY:Initilized FerryAgent")
        self.num_ferrys = num_ferrys
        self.max_size = max_size
        s = (int(np.log10((self.num_ferrys ** 2) * 200) -0.9)*self.max_size)*2 + 1
        self.grid=  np.array([max(s, size), max(s, size)])
        self.static_entetys = self.get_static_entrys(self.grid[0], max(static_entetys,self.max_size))
        # np.array([(x, self.grid[0]//2) for x in self._gen_gap(size, max(static_entetys, self.max_size))])
        self.ferry_pos = {}
        self.target_pos = {}
        self.size = {}
        self.colision = colision

    @staticmethod
    def get_static_entrys(size = 9,max_size = 2):
        """
        generates wall in the middel of environment with bottelneck
        """
        return np.array([(x, size//2) for x in FerryAgent._gen_gap(size, max_size)])

    def reset(self, seed):
        self.ferry_pos = {};  self.target_pos = {};  self.size = {}
        # init seed for randomnes
        random.seed(seed)
        rng = np.random.default_rng(seed if seed else random.randint(0,999999999))
        # generate sizes of all agents to enable colision checks
        self.size = np.array(rng.integers(low=0, high=self.max_size, size=self.num_ferrys))
        for x in range(self.num_ferrys):
            if x <self.num_ferrys //2:                  # split 50/50 to both sides
                #starting pos right side
                self.ferry_pos[x] =  np.array(rng.integers(low=[0,0], high=[self.grid[0]-1-self.size[x],(self.grid[1]//2)-1], size=2),dtype=np.float32)
                while self.check_colision(x):           # redo until vailid
                    self.ferry_pos[x] =  np.array(rng.integers(low=[0,0], high=[self.grid[0]-1-self.size[x],(self.grid[1]//2)-1], size=2),dtype=np.float32)
                # target pos left side
                self.target_pos[x] =  np.array(rng.integers(low=[0,(self.grid[1]//2)+1], high=self.grid-1-self.size[x], size=2),dtype=np.float32)
                while self.check_colision_target(x):    # redo until vailid
                    self.target_pos[x] =  np.array(rng.integers(low=[0,(self.grid[1]//2)+1], high=self.grid-1-self.size[x], size=2),dtype=np.float32)
            else:                                       # the other side 
                # starting pos left side
                self.ferry_pos[x] =  np.array(rng.integers(low=[0,(self.grid[1]//2)+1], high=self.grid-1-self.size[x], size=2),dtype=np.float32)
                while self.check_colision(x):           # redo until vailid
                    self.ferry_pos[x] =  np.array(rng.integers(low=[0,(self.grid[1]//2)+1], high=self.grid-1-self.size[x], size=2),dtype=np.float32)
                # target pos right side
                self.target_pos[x] =  np.array(rng.integers(low=[0,0], high=[self.grid[0]-1-self.size[x], (self.grid[1]//2)-1], size=2),dtype=np.float32)
                while self.check_colision_target(x):    # redo until vailid
                    self.target_pos[x] =  np.array(rng.integers(low=[0,0], high=[self.grid[0]-1-self.size[x], (self.grid[1]//2)-1], size=2),dtype=np.float32)
                    

        # check that all ferrys are correctly generated without collisions
        for ferry_id in range(self.num_ferrys):
            if self.check_colision(ferry_id=ferry_id):
                logging.error(f"Reset is not correct")
                return False



    def colision_wall_target(self, ferry_id: int, ) -> bool:
        """
        colision check for target position i.e. valid pos with wall
        """
        border_colision = False
        for x in range(len(self.static_entetys)): 
            top_left = (self.target_pos[ferry_id] == self.static_entetys[x]).all()
            top_right = (self.target_pos[ferry_id] + np.array([self.size[ferry_id],0]) == self.static_entetys[x]).all()
            bottom_left = (self.target_pos[ferry_id] + np.array([0,self.size[ferry_id]]) == self.static_entetys[x]).all()
            bottom_right = (self.target_pos[ferry_id] + np.array([self.size[ferry_id]]) == self.static_entetys[x]).all()
            border_colision = border_colision or top_left or top_right or bottom_left or bottom_right
        return border_colision
    
    def colision_ships_target(self, ferry_id: int, ) -> bool:
        """
        colision check for target position i.e. valid pos with other target postions when ships reached them
        """
        # colision check for over ships
        entety_colision = False
        pos_range = [[],[]]
        for x in [0,1]:
            # +1 is needed for range as size starts at 0
            pos_range[x] = range(int(self.target_pos[ferry_id][x]), int(self.target_pos[ferry_id][x] + self.size[ferry_id] +1))
        for colider in range(ferry_id): #  range(len(self.target_pos)):
            if not colider == ferry_id and not entety_colision:
                colider_range = [[],[]]; axes = np.array([False,False])
                #create ranges for all positions given the sizes
                # create colision values for each axis
                for x in [0,1]:
                    # +1 is needed for range as size starts at 0
                    colider_range[x] = range(int(self.target_pos[colider][x]), int(self.target_pos[colider][x] + self.size[colider] +1))
                    axes[x] = bool(set(pos_range[x]).intersection(colider_range[x]))
                # combine all colision values for the colider ship and current ship
                entety_colision = entety_colision or axes.all()
        return entety_colision
        
    def check_colision_target(self ,ferry_id: int) -> bool:        
        """
        colision check for target position i.e. valid pos with respect to out of bound,  other target positions, wall
        use self.colision = [True,True,True] to toggel colision with any of these.
        """
        out_of_bounds = (self.target_pos[ferry_id] + self.size[ferry_id] > self.grid-1).any() or (self.target_pos[ferry_id] < [0,0]).any()
        # colision check for over ships
        entety_colision = self.colision_ships_target(ferry_id)
        # colsion check for middel wall
        border_colision = self.colision_wall_target(ferry_id)
        # print(f"out_of_bounds {out_of_bounds}; entety_colision {entety_colision}; border_colision {border_colision}")
        return np.logical_and(np.array([out_of_bounds, entety_colision, border_colision]), self.colision).any()



    def take_action(self, action:Action, ferry_id:int) -> bool:
        match action:
            case Action.LEFT: 
                self.ferry_pos[ferry_id][1] -= 1
            case Action.RIGHT:
                self.ferry_pos[ferry_id][1] += 1
            case Action.UP: 
                self.ferry_pos[ferry_id][0] -= 1
            case Action.DOWN:
                self.ferry_pos[ferry_id][0] += 1
            # case Action.NOTHING:
            #     pass
            case _:
                # logging.warning("FERRY:That was not a valid action")
                print("invalid action")

        # TODO check for corectnes somtimes its not workign somtimes it is
        target_achived =  all(
                [bool(set(range(
                        int(self.ferry_pos[ferry_id][x]), int(self.ferry_pos[ferry_id][x] + self.size[ferry_id])
                    )).intersection(
                        [int(self.target_pos[ferry_id][x])])) 
                    for x in [0,1]]
                ) # (self.ferry_pos[ferry_id] == self.target_pos[ferry_id]).all()
        if target_achived:
            # logging.info("FERRY:target achieved")
            # print("target achieved")
            pass

        return target_achived
    
    def reverse_action(self,action:Action, ferry_id:int)->bool:
        # print("reveersing")
        match action:
            case Action.LEFT:
                self.take_action(Action.RIGHT, ferry_id)
                #logging.info("L")
            case Action.RIGHT:
                self.take_action(Action.LEFT, ferry_id)
                #logging.info("R")
            case Action.DOWN:
                self.take_action(Action.UP, ferry_id)
                #logging.info("U")
            case Action.UP:
                self.take_action(Action.DOWN, ferry_id)
                #logging.info("D")
            case _:
                print(f"HA?{action}")
        # logging.debug("collision")
        # print("colision")

        pass

    def colision_wall(self, ferry_id: int, ) -> bool:
        border_colision = False
        for x in range(len(self.static_entetys)): 
            top_left = (self.ferry_pos[ferry_id] == self.static_entetys[x]).all()
            top_right = (self.ferry_pos[ferry_id] + np.array([self.size[ferry_id],0]) == self.static_entetys[x]).all()
            bottom_left = (self.ferry_pos[ferry_id] + np.array([0,self.size[ferry_id]]) == self.static_entetys[x]).all()
            bottom_right = (self.ferry_pos[ferry_id] + np.array([self.size[ferry_id]]) == self.static_entetys[x]).all()
            border_colision = border_colision or top_left or top_right or bottom_left or bottom_right
        return border_colision
    
    def colision_ships(self, ferry_id: int, ) -> bool:
        entety_colision = False
        pos_range = [[],[]]
        # colision check for over ships
        pos_range = [[],[]]
        for x in [0,1]:
            # +1 is needed for range as size starts at 0
            pos_range[x] = range(int(self.ferry_pos[ferry_id][x]), int(self.ferry_pos[ferry_id][x] + self.size[ferry_id] +1))
        for colider in range(len(self.ferry_pos)):
            if not colider == ferry_id and not entety_colision:
                colider_range = [[],[]]; axes = np.array([False,False])
                #create ranges for all positions given the sizes
                # create colision values for each axis
                for x in [0,1]:
                    # +1 is needed for range as size starts at 0
                    colider_range[x] = range(int(self.ferry_pos[colider][x]), int(self.ferry_pos[colider][x] + self.size[colider] +1))
                    axes[x] = bool(set(pos_range[x]).intersection(colider_range[x]))
                # combine all colision values for the colider ship and current ship
                entety_colision = entety_colision or axes.all()
        return entety_colision
        
    def check_colision(self ,ferry_id: int) -> bool:
        out_of_bounds = (self.ferry_pos[ferry_id] + self.size[ferry_id] > self.grid-1).any() or (self.ferry_pos[ferry_id] < [0,0]).any()
        # colision check for over ships
        entety_colision = self.colision_ships(ferry_id)
        # colsion check for middel wall
        border_colision = self.colision_wall(ferry_id)
        # print(f"out_of_bounds {out_of_bounds}; entety_colision {entety_colision}; border_colision {border_colision}")
        return np.logical_and(np.array([out_of_bounds, entety_colision, border_colision]), self.colision).any()



    @staticmethod
    def _gen_gap(r, m) -> list:
        # Generate the list of indexes from 0 to r-1
        indexes = list(range(r))
        
        # Find the middle index
        mid = r // 2
        
        # Remove m elements starting from the middle
        start = mid - m // 2
        end = mid + m // 2 + m % 2  # Ensure m is properly accounted for if it's odd
        
        # Remove the range of elements from start to end
        del indexes[start:end]
        
        return indexes






def _Test_Ferry():
    ferry = FerryAgent(5,5,logging_level=logging.DEBUG)
    ferry.reset(12345)

    for i in range(25):
        ra = random.choice(list(Action))
        logging.debug(f"FERRY:taking action {ra}")
        ferry.take_action(ra, 0)

def _Test_reset():
    ferry = FerryAgent(logging_level=logging.DEBUG)
    for i in range(0,1000):
        ferry.reset(random.randint(0,999999))
        for ferry_id in [0,1]:
            if ferry.check_colision(ferry_id=ferry_id):
                logging.error(f"Reset is not correct")
                print("fuck")
                return False
    return True
# _Test_reset()