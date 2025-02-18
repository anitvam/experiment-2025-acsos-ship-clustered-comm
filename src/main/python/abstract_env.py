import numpy as np
import random
from enum import Enum
import logging
import gymnasium as gym

from ray.rllib.env.multi_agent_env import MultiAgentEnv, make_multi_agent
from ray.rllib.examples.envs.classes.cartpole_with_dict_observation_space import (CartPoleWithDictObservationSpace,)
from ray.rllib.examples.envs.classes.nested_space_repeat_after_me_env import (NestedSpaceRepeatAfterMeEnv,)
from ray.rllib.examples.envs.classes.stateless_cartpole import StatelessCartPole

import abstract_env_ferry as ferry

gym.envs.registration.register(
    id="abstract-env-ferry-v0",
    entry_point="abstract_env:Ferry",
)

class Ferry(MultiAgentEnv):#gym.Env):
    """
    Environment class for Multi Agent Reinforcment Learning (MARL)
    Args:
        config : dict
            a dict containing "rows","columns","agent_ids","logging_level" and the corresponding values.
    """

    metadata={"render_modes":[None], }

    def __init__(self, 
        config={'size':9,'agent_ids':2,
            "logging_level":logging.INFO,
            "rewards":[100, -0.1, -10,-0.05],
            "colision":[True,True,True], 
            "global_scale":0.2, 
            "random_global":0}
    ): #rows=5, columns=5, logging_level = logging.INFO):
        """
        Creates an Raylib environment for training MARL agents, This is and abstract representation.
        Args:
            config      :   dict
                a dict containg "rows"(5), "columns"(5), "agent_ids"(1), "logging_level"(logging.INFO) and its respective int sizes.
                if not given defaults will be used

                size = 9
                    default size for environment automaticly scales with number of ferrys and max size
                num_ferrys = 2
                    number of ferry agents that are created , can scale size to max of 40 agents of size 2.
                max_size = 2
                    maximum size of agents
                colision = [True, True, True]
                    out of bound,  other target positions, wall
        """
        super().__init__()
        self.grid = (config.get("size",9), config.get("size",9)) # (rows,columns)
        self.agent_ids = set(range(config.get("agent_ids",2)))
        self.ferry = ferry.FerryAgent(
            size=config.get("size",9), 
            num_ferrys=len(self.agent_ids), 
            
            colision=config.get("colision", np.array([True,True,True]))
            )
        self.grid = self.ferry.grid
        self.agents = list(self.agent_ids)
        self.possible_agents = self.agents
        # define a action space that fits to all agents i.e. [4,4,4,...]
        self.action_space_single_agent = gym.spaces.Discrete(len(ferry.Action))
        # self.action_space = gym.spaces.MultiDiscrete([len(ferry.Action) for x in range(config.get("agent_ids",1))])
        # self.action_space = self.action_space_single_agent
        self.action_space = _sspace(len(self.agents))
        # define a observation state for a single agent i.e. a 5x5 matrix
        # definition of observation space for all agents in the MARL content i.e. set of observation spaces.
        self.observation_space = {}
        shape = 2 + ((2+1) * len(self.agent_ids )) # targetPos + ((pos+size) * number of agents)
        shape = 2* len(self.agent_ids) +2 # old one without size

        self.ferry.reset(None)
        self.shape = (len(self.get_observation()[0]), ) #self.get_observation()[0].shape
        for x in range(len(self.agent_ids)):
            self.observation_space[x] = gym.spaces.Box(
                low=0,
                # 2 cords for ferry pos for every agent + 2 for own target 2
                high= int(self.grid[0] ), # + 1, # np.array(np.full((shape,), self.grid[0] + 1),dtype=np.float32),
                # high= np.array(np.full((2* len(self.agent_ids) +2,), self.grid[0] + 1),dtype=np.float32),
                shape= self.shape, #(shape,),
                # shape=(2*len(self.agent_ids) +2,),
                dtype=np.float32
            )
        
        self.observation_space = gym.spaces.Dict(self.observation_space)
        self.rewards = config.get("rewards",[100, -0.1, -10,-0.05])
        # logger = logging.getLogger(__name__)
        # logging.basicConfig(filename='logs/Abstract_env.log', encoding='utf-8', level=config.get("loggin_level",logging.INFO))
        self.max_steps = 33 
        self.print = False
        self.global_scale = config.get("global_scale", 0.2)
        self.random_global = config.get("random_global", 0)

    def reset(self, *, seed=None, options=None) -> (dict,dict):
        """
        resets the Environment and returns a observation and a info dict

        each agent has its own observation returned, as defaulted by raylib only agents that are active will recive observations
        for the next turn.

        Args:
            seed        :   int
                the seed used for geting the randomnes to be abble to reproduce stuff.
            optrions    :   dict
                options for reseting the environment, no further options available at the current moment
        returns:
            observation_state:
                dict of observation state for all agents that take an action
            info_dict:
                dict of observations
        """
        # gym requires this call to control randomness and reproduce scenarios.
        super().reset(seed=seed)
        print("reset enviornment")
        self.ferry.reset(seed=seed)
        logging.info(f"=============Reset environment=============\nSeed: {seed}")

        observation_state = self.get_observation()
        info={}
        self.possible_agents = self.agents
        # create dict of all observation states
        # observation_satate_dict = {x:observation_state for x in range(len(self.agent_ids)) }            
        info_dict = {}
        for x in self.agent_ids:
            info_dict[x] = info
        self.print = False
        self.steps = 0
        self.global_random =self.global_scale + random.uniform(-self.random_global,self.random_global)
        if np.array([not self.shape != obs for obs in observation_state]).any():
            print(f" observation space : { self.shape}, observation_state: {[obs.shape for obs in observation_state]}")
        return observation_state, info_dict

    def step(self, action_dict) -> (dict,dict,dict,dict,dict):
        """
        Steps in the environment for all agents.
        neccesary name for training

        Args:
            action_dict     :   dict
                dict of all agents taking a step with their respective actions (ferry.Action).
        returns:
            observation_state: 
                dict of observations following self.obbservation_space for all agents that take an action in the next step
            reward: 
                dict of rewards(float) for each agent that took an action
            terminated:
                dict of bools if terminated or not, if terminated no obs_state
            truncated:
                dict of bools of truncated or not ( early termination)
            info_dict:
                dict of dicts with information that might be neccesary (currently nothing)
        """
        # rewards for different actions
        global_scale =  0.2
        # init dicts
        reward ,terminated, observation_state, info_dict= {}, {agent: True for agent in self.agents}, {}, {}; truncated = {"__all__": False}
      
        # take all actions and 
        for agent in action_dict:
            # take action
            target_achived = self.ferry.take_action(ferry.Action(action_dict[agent]), ferry_id=agent)
            if (self.ferry.ferry_pos[agent] == self.ferry.target_pos[agent]).all():
                target_achived = True

            # finished or step reward
            reward[agent] = self.reward_agent(agent, target_achived, action_dict)

            #termination checks
            terminated[agent] = target_achived
            if terminated[agent]:
                logging.info(f"Reward {reward}")
            #static truncated and info returns
            info_dict[agent] = {}
            truncated[agent] = False

       
        # global reward
        global_reward = np.sum(np.array([reward[x] for x in reward.keys()]))
        for k in reward.keys():
            reward[k] = reward[k] + global_reward * self.global_random

        # only gives obs for all non finished agents.
        observation_state_helper  = self.get_observation()
        observation_state = {k:observation_state_helper[k] for k in action_dict.keys()}
        # observation_state = self.get_observation()u
        # removes all obs from finished agents
        terminated["__all__"] = True
        self.steps += 1
        if self.steps >= self.max_steps:
            for agent in action_dict:
                # print(f"Terminated agent  {agent } from { terminated[agent] } to True")
                terminated[agent] = True
        for t in terminated:
            if terminated[t]:
                self.print = True
                if t in observation_state:
                    observation_state.pop(t)
            terminated["__all__"] = bool(terminated["__all__"] and terminated[t])
        
        if terminated["__all__"]:
            logging.info(f"steps till termination {self.steps}")
        if np.array([self.shape == obs for obs in observation_state]).any():
            print(f" observation space : { self.shape}, observation_state: {[observation_state[obs].shape for obs in observation_state]}")
        # Return observation, reward, terminated, truncated (limit of max steps that can be taken), info

        # if np.array([not self.shape == obs for obs in observation_state]).any():
        #     print(f" observation space : { self.shape}, observation_state: {[obs for obs in observation_state]}")
        return observation_state, reward, terminated, truncated , info_dict

    def reward_agent(self,agent, target_achived: bool, action_dict:dict) -> float:
        # rewards for different actions
        
        reward_finished = self.rewards[0]
        scale_linear_reward = self.rewards[1]
        reward_colision = self.rewards[2]
        mid_scale = self.rewards[3]
        middel = np.array([4,3])
        
        #distance penalty
        distance_reward = np.linalg.norm(self.ferry.ferry_pos[agent] - self.ferry.target_pos[agent]) * scale_linear_reward
        #penalty for distance to opening
        opening_reward = min( np.linalg.norm(self.ferry.ferry_pos[agent] - middel), np.linalg.norm(self.ferry.ferry_pos[agent] - middel + [0,1])) * mid_scale
        # colision reward
        if self.ferry.check_colision(ferry_id=agent):
            logging.debug(f"ferry pos: {self.ferry.ferry_pos[agent]}")

            self.ferry.reverse_action(ferry.Action(action_dict[agent]), ferry_id=agent),
            return reward_colision
        else:
            #reward or penalty depending on achiving or not of the goal
            return reward_finished if target_achived else distance_reward + opening_reward #TODO alpha value instead of -0.1

    def get_observation(self):
        obs = {}
        # !!!IMPORTANT!!! fix type of box's # its important that its float32 otherwhiese there are nonsensical errors
        # for each agent crate array with all values
        for x in self.agent_ids:
            # obs[x] = np.concatenate((self.ferry.ferry_pos[x], self.ferry.target_pos[x]))
            #TODO add size
            obs[x] = np.concatenate([ self.ferry.ferry_pos[x], self.ferry.target_pos[x], np.array([self.ferry.size[x]]) ], dtype=np.float32)
            # add other ships
            for y in self.agent_ids:
                # obs[x] = np.concatenate((obs[x], self.ferry.ferry_pos[y] )) if x != y else obs[x]
                # TODO add size
                obs[x] = np.concatenate([ obs[x], self.ferry.ferry_pos[y], np.array([self.ferry.size[y]]) ], dtype=np.float32) if x != y else obs[x]

        return obs

def _sspace(agent_ids)->gym.spaces.Space:
    state_space = {}
    for x in range(agent_ids):
        state_space[x] = gym.spaces.Discrete(len(ferry.Action))
    return gym.spaces.Dict(state_space)

def _Test_env():
    """
    function to use random movment to test the environments basic functionality.
    """
    # env= gym.make("abstract-env-ferry-v0",rows=5,columns=5,logging_level=logging.DEBUG)
    env = Ferry({'agent_ids':2, "logging_level": logging.INFO})
    obs = env.reset()
    logging.info(f"Observation {obs}")
    steps = 0
    running_reward = 0
    ra = {}
    for i in range(1000):
        # sample actions
        for id in range(len(env.agent_ids)):
            ra[id] = env.action_space_single_agent.sample()
        obs,reward,terminated,_,_ = env.step(ra)
        logging.debug(f"taking a step {ra}, which gives the reward {reward}, and therfor we are terminated {terminated}")

        steps+=1
        # running_reward += reward
        
        if any(terminated.values()):
            obs = env.reset()[0]
            logging.info(f"termineaded with {terminated}")
            logging.info(f"achieved a termination, new observation {obs}")
            logging.info(f"steps till reward {steps} and total reward {running_reward}")
            steps = 0


def _Test_env_vis(id = 2):
    env = Ferry({'agent_ids':id, "logging_level": logging.INFO})
    obs = env.reset()
    obsprint(obs[0], env.grid[0])
    return env



import abstract_env_ferry as ferry
def obsprint(obs, field_size = 9):
    """
    prints a more visual version of the observation
    """
    agents = {}; targets= {}; size = {}
    print(f"Observation \n{obs}")
    # get all stats out of the array
    for agent in obs.keys():
        targets[agent] = obs[agent][2:4]
        agents[agent] = obs[agent][0:2]
        size[agent] = obs[agent][4]

    # Print a matrix of the field 
    
    field = np.zeros(shape=(field_size,field_size))
    for agent in agents:
        field[int(agents[agent][0]),int(agents[agent][1])] = agent + 1
        for s in range(0,int(size[agent])+1):
            for s1 in range(0,int(size[agent])+1):
                field[int(agents[agent][0]) + s, int(agents[agent][1]) + s1] = agent + 1
        #TODO check for max size
        # field[int(agents[agent][0] + size[agent]),int(agents[agent][1] + size[agent])] = agent + 1
        field[int(targets[agent][0]),int(targets[agent][1])] = - (agent + 1)
    # obstacle
    for obst in ferry.FerryAgent.get_static_entrys():
        field[obst[0],obst[1]] = -12
    print(f"field \n{field}")
    # print(field)

# x = _Test_env_vis()

# import pygame
# from gymnasium.utils.play import play
# def start():
#     mapping = {(pygame.K_LEFT,): ferry.Action.LEFT , (pygame.K_RIGHT,): ferry.Action.RIGHT,
#            (pygame.K_UP,): ferry.Action.UP, (pygame.K_DOWN,): ferry.Action.DOWN}
    
#     env = gym.make("abstract-env-ferry-v0")
#     play(env, keys_to_action=mapping,)
# start()


def move(env, d):
    """
    test function for manual usage trhough terminal
    """
    action = {}
    for k in d.keys():
        match d[k]:
            case 0:
                action[k] = ferry.Action.LEFT
            case 1:
                action[k] = ferry.Action.DOWN
            case 2:
                action[k] = ferry.Action.RIGHT
            case 3:
                action[k] = ferry.Action.UP

    x = env.step(action)
    obsprint(x[0])
    return x

def print_movement():
    """
    reminder of what numnber is which direction
    """
    print(f"  3\n0 + 2\n  1")

# print_movement()

def _Test_termination(env):
    """
    test function if it terminates
    """
    t = env.ferry.target_pos
    a = {}
    for id in t.keys():
        t[id] += 1
        a[id] = ferry.Action.LEFT
        print(t)
    env.ferry.ferry_pos = t
    obs,reward,terminated,truncated,_ = env.step(a)
    print(f"test_termination")
    obsprint(obs)
    print(terminated)
    

# _Test_termination(x)

def test():
    """
    test function with random actions of the environment
    """
    terminated = {"__all__":False}
    i = 0
    obs ,_ = x.reset()
    while not terminated["__all__"] and i <= 25:
            if 0 in obs.keys():
                if obs[0][0] > 3:
                    act = ferry.Action.UP
                elif obs[0][0] < 3:
                    act = ferry.Action.DOWN
                else:
                    print(f" oby obs {obs[0][1]} , {obs [0][3]}")
                    if obs[0][1] < obs [0][3]:
                        print(f"oby")
                        act =ferry.Action.RIGHT
                    else:
                        if obs[0][0] < obs[0][3]:
                            act = ferry.Action.DOWN
                        else:
                            act = ferry.Action.UP
                obs, rew, terminated,truncated,info =x.step({0:act, 1:ferry.Action.UP})
            else:
                if obs[1][0] > 3:
                    act = ferry.Action.UP
                elif obs[1][0] < 3:
                    act = ferry.Action.DOWN
                else:
                    print(f" oby obs {obs[1][1]} , {obs [1][3]}")
                    if obs[1][1] < obs [1][3]:
                        print(f"oby")
                        act =ferry.Action.RIGHT
                    else:
                        if obs[1][0] < obs[1][3]:
                            act = ferry.Action.DOWN
                        else:
                            act = ferry.Action.UP
                obs, rew, terminated,truncated,info =x.step({1:act})
            obsprint(obs)
            print(terminated)
            print(rew)
            i +=1

# test()