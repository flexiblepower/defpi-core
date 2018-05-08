from .{{service.class}} import {{service.class}}
from defpi.ServiceMain import ServiceMain

{{service.managerimports}}

if __name__ == "__main__":
    ServiceMain().main({{service.class}})
