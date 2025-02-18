FROM python:3.12
LABEL maintainer "Leif Schillhorn stu215788@mail.uni-kiel.de"
LABEL version "1.0"

RUN adduser --disabled-password --gecos '' lschillhorn
RUN adduser lschillhorn sudo
RUN echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers
RUN chmod -R 777 /home/lschillhorn
USER root


WORKDIR /ColabNav-NOK/
COPY abstract_env_ferry.py abstract_env.py abstract_trainer.py requierments.txt /ColabNav-NOK/
RUN pip3 install -r requierments.txt
COPY unbatch_to_individual_items.py /usr/local/lib/python3.12/site-packages/ray/rllib/connectors/module_to_env/
EXPOSE 6006
# EXPOSE PORTS FOR JUPYTER AND TENSORBOARD AND RUN THEM IF NEEDED
EXPOSE 8899
EXPOSE 6078
ENV MPLCONFIGDIR="/lschillhorn/.config_mat"

# ENTRYPOINT [ "python", "--version" ]
ENTRYPOINT [ "python","abstract_trainer.py" ]