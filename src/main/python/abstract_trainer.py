import abstract_env as ferry_env

from ray.rllib.algorithms.ppo import PPOConfig
from ray.rllib.policy.policy import PolicySpec

import numpy as np
import gymnasium as gym
import abstract_env_ferry as ferry_agent
import ray
from ray import tune
from ray.rllib.algorithms.ppo import PPOConfig
from ray.rllib.algorithms.callbacks import MemoryTrackingCallbacks
from ray.rllib.models.torch.torch_modelv2 import TorchModelV2
from ray.rllib.models import ModelCatalog

# ModelCatalog.register_custom_model("my_torch_model", CustomTorchModel)

def policy_mapping_fn(agent_id, episode, **kwargs):
    agent_idx = int(agent_id)  # 0 (player1) or 1 (player2)
    # agent_id = "player[1|2]" -> policy depends on episode ID
    # This way, we make sure that both policies sometimes play player1
    # (start player) and sometimes player2 (player to move 2nd).
    # print(f"episode = {episode}")
    return "learning_policy"

# Construct a generic config object, specifying values within different
# sub-categories, e.g. "training".
config = (PPOConfig()
        .training(
            gamma=0.7, # tune.grid_search([0.7,0.8,0.6]), 
            lr= 0.005, # tune.loguniform( 1e-4, 5e-3), 
            clip_param=0.2, # tune.uniform(0.1,0.3),  
            model={"fcnet_hiddens" : [96,96]}, # [tune.grid_search([128,64,96,160]),tune.grid_search([96,64,32,16,160])]}, 
        )
        .environment(env=ferry_env.Ferry, 
            env_config={
                "size":9,
                "agent_ids":tune.grid_search(range(2,40,2)),
                "rewards":[100, -0.1, -10,-0.05], 
                # [out_of_bounds, entety_colision, border_colision]
                "colision":np.array([True,True,True]),
                "global_scale": 0.3 ,# tune.uniform(0.2, 0.4), 
                "random_global":0.1,
                },       
        )
        .resources(num_gpus=1)
        .env_runners(num_env_runners=2)
        # .callbacks(MemoryTrackingCallbacks)
        # .learners(num_learners = 2)
        .multi_agent(
            count_steps_by="env_steps",
            policies={
                    "learning_policy": PolicySpec(),  # <- use default class & infer obs-/act-spaces from env.
                },
                policy_mapping_fn = policy_mapping_fn,
                policies_to_train = ["learning_policy"],
            )
    )
ray.init()

print(f"\n\n\n")
import os 

from tensorboard import program

# tracking_address = "/root/ray_results" # the path of your log file.

# if __name__ == "__main__":
#     tb = program.TensorBoard()
#     tb.configure(argv=[None, '--logdir', tracking_address, '--port', '6006' , '--bind_all'])
#     url = tb.launch()
#     print(f"Tensorflow listening on {url}")

from ray import train, tune
tuner = tune.Tuner(
    "PPO",
    param_space=config,
    run_config=train.RunConfig(
        stop={"training_iteration": 100, }, # "mean_accuracy": 0.8} ,
        checkpoint_config=train.CheckpointConfig(checkpoint_at_end=True, checkpoint_frequency=10, ),
        storage_path="/lschillhorn/ray_results",
    ),
    
    # checkpoint_freq=10,
)
result = tuner.fit( )
print(result)
# print(result.get_best_result(
#     metric="env_runners/episode_return_mean", mode="max"
# )
# )
best_result = result.get_best_result(
    metric="env_runners/episode_return_mean", mode="max"
)
print(best_result.checkpoint)

