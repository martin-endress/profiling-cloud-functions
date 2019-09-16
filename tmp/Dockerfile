FROM archlinux/base

RUN pacman --noconfirm -Syu
RUN pacman --noconfirm -S sysbench

CMD sysbench --test=cpu --cpu-max-prime=10000 run
