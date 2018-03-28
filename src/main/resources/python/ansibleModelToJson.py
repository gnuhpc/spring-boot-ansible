#!/usr/bin/env python
# -*- coding=utf-8 -*-
import json,sys,os,argparse,ast
from collections import namedtuple
from ansible.parsing.dataloader import DataLoader
from ansible.vars import VariableManager
from ansible.inventory import Inventory,Host,Group
from ansible.playbook.play import Play
from ansible.executor.task_queue_manager import TaskQueueManager
from ansible.plugins.callback import CallbackBase
from ansible.executor.playbook_executor import PlaybookExecutor

class ModelResultsCollector(CallbackBase):

    def __init__(self, *args, **kwargs):
        super(ModelResultsCollector, self).__init__(*args, **kwargs)
        self.host_ok = {}
        self.host_unreachable = {}
        self.host_failed = {}

    def v2_runner_on_unreachable(self, result):
        self.host_unreachable[result._host.get_name()] = result

    def v2_runner_on_ok(self, result,  *args, **kwargs):
        self.host_ok[result._host.get_name()] = result

    def v2_runner_on_failed(self, result,  *args, **kwargs):
        self.host_failed[result._host.get_name()] = result

class ANSRunner(object):
    def __init__(self,*args, **kwargs):
        self.inventory = None
        self.variable_manager = None
        self.host_list = None
        self.loader = None
        self.options = None
        self.passwords = None
        self.callback = None
        self.project_name = None
        self.results_raw = {}

    def run_model(self, host_list, module_name, module_args, Options_val, options_val):
        # 鐢ㄦ潵鍔犺浇瑙ｆ瀽yaml鏂囦欢鎴朖SON鍐呭,骞朵笖鏀寔vault鐨勮В瀵�
        self.loader = DataLoader()

        # 绠＄悊鍙橀噺鐨勭被锛屽寘鎷富鏈猴紝缁勶紝鎵╁睍绛夊彉閲忥紝涔嬪墠鐗堟湰鏄湪 inventory涓殑
        self.variable_manager = VariableManager()

        self.inventory = Inventory(loader=self.loader, variable_manager=self.variable_manager, host_list=host_list)
        self.variable_manager.set_inventory(self.inventory)
        """
        run module from andible ad-hoc.
        module_name: ansible module_name
        module_args: ansible module args
        """
        Options = Options_val
        self.options = options_val
        self.options = options
        play_source = dict(
                name="Ansible Play",
                hosts=host_list,
                gather_facts='no',
                tasks=[dict(action=dict(module=module_name, args=module_args))]
        )
        play = Play().load(play_source, variable_manager=self.variable_manager, loader=self.loader)
        tqm = None
        self.callback = ModelResultsCollector()
        try:
            tqm = TaskQueueManager(
                    inventory=self.inventory,
                    variable_manager=self.variable_manager,
                    loader=self.loader,
                    options=self.options,
                    passwords=self.passwords,
            )
            tqm._stdout_callback = self.callback
            result = tqm.run(play)
        finally:
            if tqm is not None:
                tqm.cleanup()

    def get_model_result(self):
        self.results_raw = {'success':{}, 'failed':{}, 'unreachable':{}}
        for host, result in self.callback.host_ok.items():
            self.results_raw['success'][host] = result._result

        for host, result in self.callback.host_failed.items():
            self.results_raw['failed'][host] = result._result

        for host, result in self.callback.host_unreachable.items():
            self.results_raw['unreachable'][host]= result._result
        return json.dumps(self.results_raw, indent=4)

    def get_playbook_result(self):
        self.results_raw = {'skipped':{}, 'failed':{}, 'ok':{},"status":{},'unreachable':{}}

        for host, result in self.callback.task_ok.items():
            self.results_raw['ok'][host] = result

        for host, result in self.callback.task_failed.items():
            self.results_raw['failed'][host] = result

        for host, result in self.callback.task_status.items():
            self.results_raw['status'][host] = result

        for host, result in self.callback.task_skipped.items():
            self.results_raw['skipped'][host] = result

        for host, result in self.callback.task_unreachable.items():
            self.results_raw['unreachable'][host] = result._result
        return json.dumps(self.results_raw, indent=4)



if __name__ == '__main__':
    reload(sys)
    sys.setdefaultencoding('utf8')
    parser = argparse.ArgumentParser(description='Process some integers.')
    
    # python AnaylizePlaybookResult.py 
    # -host '["192.168.1.34", "192.168.1.130", "192.168.1.1"]' 
    # -moduleName 'ping'  
    # -moduleArgs '' 
    # -o '{"connection":"smart","remote_user":"wenqiao","ack_pass":"None","sudo_user":"wenqiao","forks":"5","sudo":"no","ask_sudo_pass":"False","verbosity":"5","module_path":"None","become":"False","become_method":"sudo","become_user":"wenqiao","check":"None","listhosts":"None","listtasks":"None","listtags":"None","syntax":"None"}'

    parser.add_argument('-host', dest='hostList', type=str, help='input host list')
    parser.add_argument('-moduleName', dest='moduleName', type=str, help='input module name')
    parser.add_argument('-moduleArgs', dest='moduleArgs', type=str, help='input module args')
    parser.add_argument('-o', dest='options', type=json.loads, help='input options')
    args = parser.parse_args()

    host_list = args.hostList
    module_name = args.moduleName
    module_args = args.moduleArgs
    options_para = args.options

    # 鍒濆鍖栭渶瑕佺殑瀵硅薄
    Options = namedtuple('Options',
                         ['connection',
                          'remote_user',
                          'ask_sudo_pass',
                        'verbosity',
                          'ack_pass',
                          'module_path',
                          'forks',
                          'become',
                          'become_method',
                          'become_user',
                          'check',
                          'listhosts',
                          'listtasks',
                          'listtags',
                          'syntax',
                          'sudo_user',
                          'sudo'])
    # 鍒濆鍖栭渶瑕佺殑瀵硅薄
    options = Options(connection=options_para['connection'],
                           remote_user=options_para['remote_user'],
                           ack_pass=options_para['ack_pass'],
                           sudo_user=options_para['sudo_user'],
                           forks=options_para['forks'],
                           sudo=options_para['sudo'],
                           ask_sudo_pass=options_para['ask_sudo_pass'],
                           verbosity=options_para['verbosity'],
                           module_path=options_para['module_path'],
                           become=options_para['become'],
                           become_method=options_para['become_method'],
                           become_user=options_para['become_user'],
                           check=options_para['check'],
                           listhosts=options_para['listhosts'],
                           listtasks=options_para['listtasks'],
                           listtags=options_para['listtags'],
                           syntax=options_para['syntax'])

    # options = Options(connection='smart',
    #                   remote_user='wenqiao',
    #                   ack_pass=None,
    #                   sudo_user='wenqiao',
    #                   forks=5,
    #                   sudo='no',
    #                   ask_sudo_pass=False,
    #                   verbosity=5,
    #                   module_path=None,
    #                   become=False,
    #                   become_method='sudo',
    #                   become_user='wenqiao',
    #                   check=None,
    #                   listhosts=None,
    #                   listtasks=None,
    #                   listtags=None,
    #                   syntax=None)

    rbt = ANSRunner()
    rbt.run_model(host_list=host_list, module_name=module_name, module_args=module_args, Options_val=Options, options_val=options)

    data = rbt.get_model_result()

    print data
