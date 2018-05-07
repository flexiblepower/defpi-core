import glob
from importlib import import_module

from .{{service.class}} import {{service.class}}
from defpi.ServiceMain import ServiceMain

if __name__ == "__main__":
    for name in glob.glob('*/*ConnectionManagerImpl.py'):
        manager = name[:-3].split('/')
        module = import_module('.'.join(manager))
        getattr(module, manager[1])
    ServiceMain().main({{service.class}})
