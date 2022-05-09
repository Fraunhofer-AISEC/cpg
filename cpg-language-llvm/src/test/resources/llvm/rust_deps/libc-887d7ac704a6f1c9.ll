; ModuleID = 'libc.43510362-cgu.0'
source_filename = "libc.43510362-cgu.0"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

%"unix::linux_like::linux::in6_pktinfo" = type { %"unix::align::in6_addr", i32 }
%"unix::align::in6_addr" = type { [16 x i8] }
%"unix::linux_like::ip_mreq" = type { %"unix::linux_like::in_addr", %"unix::linux_like::in_addr" }
%"unix::linux_like::in_addr" = type { i32 }
%"unix::linux_like::ip_mreq_source" = type { %"unix::linux_like::in_addr", %"unix::linux_like::in_addr", %"unix::linux_like::in_addr" }
%"unix::linux_like::arphdr" = type { i16, i16, i8, i8, i16 }
%"unix::linux_like::linux::fsid_t" = type { [2 x i32] }
%"unix::linux_like::linux::packet_mreq" = type { i32, i16, i16, [8 x i8] }
%"unix::linux_like::linux::input_id" = type { i16, i16, i16, i16 }
%"unix::linux_like::linux::ff_envelope" = type { i16, i16, i16, i16 }
%"unix::linux_like::linux::ff_condition_effect" = type { i16, i16, i16, i16, i16, i16 }
%"unix::linux_like::linux::uinput_ff_erase" = type { i32, i32, i32 }
%"unix::linux_like::linux::Elf32_Sym" = type { i32, i32, i32, i8, i8, i16 }
%"unix::linux_like::linux::ucred" = type { i32, i32, i32 }
%"unix::linux_like::linux::inotify_event" = type { i32, i32, i32, i32 }
%"unix::linux_like::linux::sockaddr_vm" = type { i16, i16, i32, i32, [4 x i8] }
%"unix::linux_like::linux::sock_extended_err" = type { i32, i8, i8, i8, i8, i32, i32 }
%"unix::linux_like::linux::input_mask" = type { i32, i32, i64 }
%"unix::linux_like::linux::sock_filter" = type { i16, i8, i8, i32 }
%"unix::linux_like::linux::nlmsghdr" = type { i32, i16, i16, i32, i32 }
%"unix::linux_like::linux::sockaddr_nl" = type { i16, i16, i32, i32 }
%"unix::linux_like::linux::__c_anonymous_sockaddr_can_can_addr" = type { [2 x i64] }
%"unix::linux_like::linux::gnu::statx_timestamp" = type { i64, i32, [1 x i32] }
%"unix::linux_like::linux::gnu::cmsghdr" = type { i64, i32, i32 }
%"unix::linux_like::linux::gnu::nl_mmap_req" = type { i32, i32, i32, i32 }
%"unix::linux_like::linux::gnu::Elf32_Chdr" = type { i32, i32, i32 }
%"unix::linux_like::sockaddr" = type { i16, [14 x i8] }
%"unix::linux_like::sockaddr_in" = type { i16, i16, %"unix::linux_like::in_addr", [8 x i8] }
%"unix::linux_like::linux::gnu::b64::x86_64::ip_mreqn" = type { %"unix::linux_like::in_addr", %"unix::linux_like::in_addr", i32 }
%"unix::linux_like::linux::pthread_rwlockattr_t" = type { [8 x i8] }
%"unix::linux_like::linux::pthread_mutexattr_t" = type { [4 x i8] }
%"unix::linux_like::linux::can_frame" = type { i32, i8, i8, i8, i8, [8 x i8] }
%"unix::linux_like::sched_param" = type { i32 }
%"unix::linux_like::linux::af_alg_iv" = type { i32, [0 x i8] }
%"unix::linux_like::linux::gnu::nl_pktinfo" = type { i32 }
%"unix::servent" = type { i8*, i8**, i32, [1 x i32], i8* }
%"unix::linux_like::Dl_info" = type { i8*, i8*, i8*, i8* }
%"unix::linux_like::linux::itimerspec" = type { { i64, i64 }, { i64, i64 } }
%"unix::linux_like::linux::cpu_set_t" = type { [16 x i64] }
%"unix::linux_like::linux::dirent64" = type { i64, i64, i16, i8, [256 x i8], [5 x i8] }
%"unix::linux_like::linux::gnu::glob64_t" = type { i64, i8**, i64, i32, [1 x i32], i8*, i8*, i8*, i8*, i8* }
%"unix::linux_like::linux::gnu::mallinfo" = type { i32, i32, i32, i32, i32, i32, i32, i32, i32, i32 }
%"unix::linux_like::linux::gnu::nl_mmap_hdr" = type { i32, i32, i32, i32, i32, i32 }
%"unix::linux_like::linux::gnu::seminfo" = type { i32, i32, i32, i32, i32, i32, i32, i32, i32, i32 }
%"unix::linux_like::linux::gnu::b64::sigset_t" = type { [16 x i64] }
%"unix::linux_like::linux::gnu::b64::x86_64::flock64" = type { i16, i16, [2 x i16], i64, i64, i32, [1 x i32] }
%"unix::linux_like::linux::gnu::b64::x86_64::stat64" = type { i64, i64, i64, i32, i32, i32, i32, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, [3 x i64] }
%"unix::linux_like::linux::gnu::b64::x86_64::statvfs64" = type { i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, [6 x i32] }
%"unix::FILE" = type { [0 x i8] }
%"unix::fpos_t" = type { [0 x i8] }
%"unix::linux_like::timezone" = type { [0 x i8] }
%"unix::linux_like::linux::fpos64_t" = type { [0 x i8] }
%"unix::DIR" = type { [0 x i8] }
%"unix::group" = type { i8*, i8*, i32, [1 x i32], i8** }
%"unix::rusage" = type { { i64, i64 }, { i64, i64 }, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64 }
%"unix::ipv6_mreq" = type { %"unix::align::in6_addr", i32 }
%"unix::hostent" = type { i8*, i8**, i32, i32, i8** }
%"unix::pollfd" = type { i32, i16, i16 }
%"unix::winsize" = type { i16, i16, i16, i16 }
%"unix::sigval" = type { i8* }
%"unix::itimerval" = type { { i64, i64 }, { i64, i64 } }
%"unix::tms" = type { i64, i64, i64, i64 }
%"unix::protoent" = type { i8*, i8**, i32, [1 x i32] }
%"unix::linux_like::sockaddr_in6" = type { i16, i16, i32, %"unix::align::in6_addr", i32 }
%"unix::linux_like::addrinfo" = type { i32, i32, i32, i32, i32, [1 x i32], %"unix::linux_like::sockaddr"*, i8*, %"unix::linux_like::addrinfo"* }
%"unix::linux_like::sockaddr_ll" = type { i16, i16, i32, i16, i8, i8, [8 x i8] }
%"unix::linux_like::fd_set" = type { [16 x i64] }
%"unix::linux_like::tm" = type { i32, i32, i32, i32, i32, i32, i32, i32, i32, [1 x i32], i64, i8* }
%"unix::linux_like::lconv" = type { i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8*, i8, i8, i8, i8, i8, i8, i8, i8, i8, i8, i8, i8, i8, i8, [2 x i8] }
%"unix::linux_like::in_pktinfo" = type { i32, %"unix::linux_like::in_addr", %"unix::linux_like::in_addr" }
%"unix::linux_like::ifaddrs" = type { %"unix::linux_like::ifaddrs"*, i8*, i32, [1 x i32], %"unix::linux_like::sockaddr"*, %"unix::linux_like::sockaddr"*, %"unix::linux_like::sockaddr"*, i8* }
%"unix::linux_like::in6_rtmsg" = type { %"unix::align::in6_addr", %"unix::align::in6_addr", %"unix::align::in6_addr", i32, i16, i16, i32, [1 x i32], i64, i32, i32 }
%"unix::linux_like::arpreq" = type { %"unix::linux_like::sockaddr", %"unix::linux_like::sockaddr", i32, %"unix::linux_like::sockaddr", [16 x i8] }
%"unix::linux_like::arpreq_old" = type { %"unix::linux_like::sockaddr", %"unix::linux_like::sockaddr", i32, %"unix::linux_like::sockaddr" }
%"unix::linux_like::mmsghdr" = type { %"unix::linux_like::linux::gnu::msghdr", i32, [1 x i32] }
%"unix::linux_like::linux::gnu::msghdr" = type { i8*, i32, [1 x i32], { i8*, i64 }*, i64, i8*, i64, i32, [1 x i32] }
%"unix::linux_like::epoll_event" = type <{ i32, i64 }>
%"unix::linux_like::sockaddr_un" = type { i16, [108 x i8] }
%"unix::linux_like::sockaddr_storage" = type { i16, [3 x i16], i64, [112 x i8] }
%"unix::linux_like::utsname" = type { [65 x i8], [65 x i8], [65 x i8], [65 x i8], [65 x i8], [65 x i8] }
%"unix::linux_like::sigevent" = type { %"unix::sigval", i32, i32, i32, [11 x i32] }
%"unix::linux_like::linux::glob_t" = type { i64, i8**, i64, i32, [1 x i32], i8*, i8*, i8*, i8*, i8* }
%"unix::linux_like::linux::passwd" = type { i8*, i8*, i32, i32, i8*, i8*, i8* }
%"unix::linux_like::linux::spwd" = type { i8*, i8*, i64, i64, i64, i64, i64, i64, i64 }
%"unix::linux_like::linux::dqblk" = type { i64, i64, i64, i64, i64, i64, i64, i64, i32, [1 x i32] }
%"unix::linux_like::linux::signalfd_siginfo" = type { i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i64, i64, i64, i64, i16, i16, i32, i64, i32, [28 x i8] }
%"unix::linux_like::linux::msginfo" = type { i32, i32, i32, i32, i32, i32, i32, i16, [1 x i16] }
%"unix::linux_like::linux::sembuf" = type { i16, i16, i16 }
%"unix::linux_like::linux::input_event" = type { { i64, i64 }, i16, i16, i32 }
%"unix::linux_like::linux::input_absinfo" = type { i32, i32, i32, i32, i32, i32 }
%"unix::linux_like::linux::input_keymap_entry" = type { i8, i8, i16, i32, [32 x i8] }
%"unix::linux_like::linux::ff_constant_effect" = type { i16, %"unix::linux_like::linux::ff_envelope" }
%"unix::linux_like::linux::ff_ramp_effect" = type { i16, i16, %"unix::linux_like::linux::ff_envelope" }
%"unix::linux_like::linux::ff_periodic_effect" = type { i16, i16, i16, i16, i16, %"unix::linux_like::linux::ff_envelope", [1 x i16], i32, i16* }
%"unix::linux_like::linux::ff_effect" = type { i16, i16, i16, { i16, i16 }, { i16, i16 }, [1 x i16], [4 x i64] }
%"unix::linux_like::linux::uinput_ff_upload" = type { i32, i32, %"unix::linux_like::linux::ff_effect", %"unix::linux_like::linux::ff_effect" }
%"unix::linux_like::linux::uinput_abs_setup" = type { i16, [1 x i16], %"unix::linux_like::linux::input_absinfo" }
%"unix::linux_like::linux::dl_phdr_info" = type { i64, i8*, %"unix::linux_like::linux::Elf64_Phdr"*, i16, [3 x i16], i64, i64, i64, i8* }
%"unix::linux_like::linux::Elf64_Phdr" = type { i32, i32, i64, i64, i64, i64, i64, i64 }
%"unix::linux_like::linux::Elf32_Ehdr" = type { [16 x i8], i16, i16, i32, i32, i32, i32, i32, i16, i16, i16, i16, i16, i16 }
%"unix::linux_like::linux::Elf64_Ehdr" = type { [16 x i8], i16, i16, i32, i64, i64, i64, i32, i16, i16, i16, i16, i16, i16 }
%"unix::linux_like::linux::Elf64_Sym" = type { i32, i8, i8, i16, i64, i64 }
%"unix::linux_like::linux::Elf32_Phdr" = type { i32, i32, i32, i32, i32, i32, i32, i32 }
%"unix::linux_like::linux::Elf32_Shdr" = type { i32, i32, i32, i32, i32, i32, i32, i32, i32, i32 }
%"unix::linux_like::linux::Elf64_Shdr" = type { i32, i32, i64, i64, i64, i64, i32, i32, i64, i64 }
%"unix::linux_like::linux::mntent" = type { i8*, i8*, i8*, i8*, i32, i32 }
%"unix::linux_like::linux::posix_spawn_file_actions_t" = type { i32, i32, i32*, [16 x i32] }
%"unix::linux_like::linux::posix_spawnattr_t" = type { i16, [1 x i16], i32, %"unix::linux_like::linux::gnu::b64::sigset_t", %"unix::linux_like::linux::gnu::b64::sigset_t", %"unix::linux_like::sched_param", i32, [16 x i32] }
%"unix::linux_like::linux::genlmsghdr" = type { i8, i8, i16 }
%"unix::linux_like::linux::arpd_request" = type { i16, [1 x i16], i32, i64, i64, i64, [7 x i8], [1 x i8] }
%"unix::linux_like::linux::__c_anonymous_sockaddr_can_j1939" = type { i64, i32, i8, [3 x i8] }
%"unix::linux_like::linux::seccomp_data" = type { i32, i32, i64, [6 x i64] }
%"unix::linux_like::linux::nlmsgerr" = type { i32, %"unix::linux_like::linux::nlmsghdr" }
%"unix::linux_like::linux::dirent" = type { i64, i64, i16, i8, [256 x i8], [5 x i8] }
%"unix::linux_like::linux::sockaddr_alg" = type { i16, [14 x i8], i32, i32, [64 x i8] }
%"unix::linux_like::linux::uinput_setup" = type { %"unix::linux_like::linux::input_id", [80 x i8], i32 }
%"unix::linux_like::linux::uinput_user_dev" = type { [80 x i8], %"unix::linux_like::linux::input_id", i32, [64 x i32], [64 x i32], [64 x i32], [64 x i32] }
%"unix::linux_like::linux::mq_attr" = type { i64, i64, i64, i64, [4 x i64] }
%"unix::linux_like::linux::sockaddr_can" = type { i16, [1 x i16], i32, %"unix::linux_like::linux::__c_anonymous_sockaddr_can_can_addr" }
%"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t" = type { i32, i32, i32, [29 x i32], [0 x i64] }
%"unix::linux_like::linux::gnu::statx" = type { i32, i32, i64, i32, i32, i32, i16, [1 x i16], i64, i64, i64, i64, %"unix::linux_like::linux::gnu::statx_timestamp", %"unix::linux_like::linux::gnu::statx_timestamp", %"unix::linux_like::linux::gnu::statx_timestamp", %"unix::linux_like::linux::gnu::statx_timestamp", i32, i32, i32, i32, i64, i64, [12 x i64] }
%"unix::linux_like::linux::gnu::aiocb" = type { i32, i32, i32, [1 x i32], i8*, i64, %"unix::linux_like::sigevent", %"unix::linux_like::linux::gnu::aiocb"*, i32, i32, i32, [1 x i32], i64, i64, [32 x i8] }
%"unix::linux_like::linux::gnu::termios" = type { i32, i32, i32, i32, i8, [32 x i8], [3 x i8], i32, i32 }
%"unix::linux_like::linux::gnu::mallinfo2" = type { i64, i64, i64, i64, i64, i64, i64, i64, i64, i64 }
%"unix::linux_like::linux::gnu::rtentry" = type { i64, %"unix::linux_like::sockaddr", %"unix::linux_like::sockaddr", %"unix::linux_like::sockaddr", i16, i16, [2 x i16], i64, i8, i8, [3 x i16], i16, [3 x i16], i8*, i64, i64, i16, [3 x i16] }
%"unix::linux_like::linux::gnu::timex" = type { i32, [1 x i32], i64, i64, i64, i64, i32, [1 x i32], i64, i64, i64, { i64, i64 }, i64, i64, i64, i32, [1 x i32], i64, i64, i64, i64, i64, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32, i32 }
%"unix::linux_like::linux::gnu::ntptimeval" = type { { i64, i64 }, i64, i64, i64, i64, i64, i64, i64 }
%"unix::linux_like::linux::gnu::regex_t" = type { i8*, i64, i64, i64, i8*, i8*, i64, i8, [7 x i8] }
%"unix::linux_like::linux::gnu::Elf64_Chdr" = type { i32, i32, i64, i64 }
%"unix::linux_like::linux::gnu::sifields_sigchld" = type { i32, i32, i32, [1 x i32], i64, i64 }
%"unix::linux_like::linux::gnu::utmpx" = type { i16, [1 x i16], i32, [32 x i8], [4 x i8], [32 x i8], [256 x i8], { i16, i16 }, i32, { i32, i32 }, [4 x i32], [20 x i8] }
%"unix::linux_like::linux::gnu::b64::sysinfo" = type { i64, [3 x i64], i64, i64, i64, i64, i64, i64, i16, i16, [2 x i16], i64, i64, i32, [0 x i8], [4 x i8] }
%"unix::linux_like::linux::gnu::b64::msqid_ds" = type { %"unix::linux_like::linux::gnu::b64::x86_64::ipc_perm", i64, i64, i64, i64, i64, i64, i32, i32, i64, i64 }
%"unix::linux_like::linux::gnu::b64::x86_64::ipc_perm" = type { i32, i32, i32, i32, i32, i16, i16, i16, i16, [2 x i16], i64, i64 }
%"unix::linux_like::linux::gnu::b64::semid_ds" = type { %"unix::linux_like::linux::gnu::b64::x86_64::ipc_perm", i64, i64, i64, i64, i64, i64, i64 }
%"unix::linux_like::linux::gnu::b64::x86_64::sigaction" = type { i64, %"unix::linux_like::linux::gnu::b64::sigset_t", i32, [1 x i32], i64* }
%"unix::linux_like::linux::gnu::b64::x86_64::statfs" = type { i64, i64, i64, i64, i64, i64, i64, %"unix::linux_like::linux::fsid_t", i64, i64, [5 x i64] }
%"unix::linux_like::linux::gnu::b64::x86_64::flock" = type { i16, i16, [2 x i16], i64, i64, i32, [1 x i32] }
%"unix::linux_like::linux::gnu::b64::x86_64::stack_t" = type { i8*, i32, [1 x i32], i64 }
%"unix::linux_like::linux::gnu::b64::x86_64::stat" = type { i64, i64, i64, i32, i32, i32, i32, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, [3 x i64] }
%"unix::linux_like::linux::gnu::b64::x86_64::statfs64" = type { i64, i64, i64, i64, i64, i64, i64, %"unix::linux_like::linux::fsid_t", i64, i64, i64, [4 x i64] }
%"unix::linux_like::linux::gnu::b64::x86_64::pthread_attr_t" = type { [7 x i64] }
%"unix::linux_like::linux::gnu::b64::x86_64::_libc_fpxreg" = type { [4 x i16], i16, [3 x i16] }
%"unix::linux_like::linux::gnu::b64::x86_64::_libc_xmmreg" = type { [4 x i32] }
%"unix::linux_like::linux::gnu::b64::x86_64::_libc_fpstate" = type { i16, i16, i16, i16, i64, i64, i32, i32, [8 x %"unix::linux_like::linux::gnu::b64::x86_64::_libc_fpxreg"], [16 x %"unix::linux_like::linux::gnu::b64::x86_64::_libc_xmmreg"], [12 x i64] }
%"unix::linux_like::linux::gnu::b64::x86_64::user_regs_struct" = type { i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64 }
%"unix::linux_like::linux::gnu::b64::x86_64::user" = type { %"unix::linux_like::linux::gnu::b64::x86_64::user_regs_struct", i32, [1 x i32], %"unix::linux_like::linux::gnu::b64::x86_64::user_fpregs_struct", i64, i64, i64, i64, i64, i64, i32, [1 x i32], %"unix::linux_like::linux::gnu::b64::x86_64::user_regs_struct"*, %"unix::linux_like::linux::gnu::b64::x86_64::user_fpregs_struct"*, i64, [32 x i8], [8 x i64] }
%"unix::linux_like::linux::gnu::b64::x86_64::user_fpregs_struct" = type { i16, i16, i16, i16, i64, i64, i32, i32, [32 x i32], [64 x i32], [24 x i32] }
%"unix::linux_like::linux::gnu::b64::x86_64::mcontext_t" = type { [23 x i64], %"unix::linux_like::linux::gnu::b64::x86_64::_libc_fpstate"*, [8 x i64] }
%"unix::linux_like::linux::gnu::b64::x86_64::shmid_ds" = type { %"unix::linux_like::linux::gnu::b64::x86_64::ipc_perm", i64, i64, i64, i64, i32, i32, i64, i64, i64 }
%"unix::linux_like::linux::gnu::b64::x86_64::ucontext_t" = type { i64, %"unix::linux_like::linux::gnu::b64::x86_64::ucontext_t"*, %"unix::linux_like::linux::gnu::b64::x86_64::stack_t", %"unix::linux_like::linux::gnu::b64::x86_64::mcontext_t", %"unix::linux_like::linux::gnu::b64::sigset_t", [512 x i8] }
%"unix::linux_like::linux::gnu::b64::x86_64::not_x32::statvfs" = type { i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, i64, [6 x i32] }
%"unix::linux_like::linux::gnu::b64::x86_64::align::max_align_t" = type { [4 x double] }
%"unix::linux_like::linux::gnu::align::sem_t" = type { [32 x i8] }
%"unix::linux_like::linux::arch::generic::termios2" = type { i32, i32, i32, i32, i8, [19 x i8], i32, i32 }
%"unix::linux_like::linux::pthread_condattr_t" = type { [4 x i8] }
%"unix::linux_like::linux::fanotify_event_metadata" = type { i32, i8, i8, i16, i64, i32, i32 }
%"unix::linux_like::linux::pthread_cond_t" = type { [48 x i8] }
%"unix::linux_like::linux::pthread_mutex_t" = type { [40 x i8] }
%"unix::linux_like::linux::pthread_rwlock_t" = type { [56 x i8] }
%"unix::linux_like::linux::canfd_frame" = type { i32, i8, i8, i8, i8, [64 x i8] }
%"unix::linux_like::linux::non_exhaustive::open_how" = type { i64, i64, i64 }

@"_ZN81_$LT$libc..unix..linux_like..linux..in6_pktinfo$u20$as$u20$core..clone..Clone$GT$5clone17h453ce9dcd78e105bE" = unnamed_addr alias void (%"unix::linux_like::linux::in6_pktinfo"*, %"unix::linux_like::linux::in6_pktinfo"*), bitcast (void (%"unix::ipv6_mreq"*, %"unix::ipv6_mreq"*)* @"_ZN60_$LT$libc..unix..ipv6_mreq$u20$as$u20$core..clone..Clone$GT$5clone17hbb1a5c27fc2d689fE" to void (%"unix::linux_like::linux::in6_pktinfo"*, %"unix::linux_like::linux::in6_pktinfo"*)*)
@"_ZN70_$LT$libc..unix..linux_like..ip_mreq$u20$as$u20$core..clone..Clone$GT$5clone17h6a31b6373a60396cE" = unnamed_addr alias i64 (%"unix::linux_like::ip_mreq"*), bitcast (i64 (%"unix::pollfd"*)* @"_ZN57_$LT$libc..unix..pollfd$u20$as$u20$core..clone..Clone$GT$5clone17h6bd9157dbaad32a4E" to i64 (%"unix::linux_like::ip_mreq"*)*)
@"_ZN77_$LT$libc..unix..linux_like..ip_mreq_source$u20$as$u20$core..clone..Clone$GT$5clone17hc75ce3f570cb4c83E" = unnamed_addr alias i96 (%"unix::linux_like::ip_mreq_source"*), bitcast (i96 (%"unix::linux_like::in_pktinfo"*)* @"_ZN73_$LT$libc..unix..linux_like..in_pktinfo$u20$as$u20$core..clone..Clone$GT$5clone17ha5fe88908abbcc3bE" to i96 (%"unix::linux_like::ip_mreq_source"*)*)
@"_ZN69_$LT$libc..unix..linux_like..arphdr$u20$as$u20$core..clone..Clone$GT$5clone17hdba13be3c42e40c5E" = unnamed_addr alias i64 (%"unix::linux_like::arphdr"*), bitcast (i64 (%"unix::winsize"*)* @"_ZN58_$LT$libc..unix..winsize$u20$as$u20$core..clone..Clone$GT$5clone17hb71a111626587a88E" to i64 (%"unix::linux_like::arphdr"*)*)
@"_ZN76_$LT$libc..unix..linux_like..linux..fsid_t$u20$as$u20$core..clone..Clone$GT$5clone17h8e93a8c4f0f1cd87E" = unnamed_addr alias i64 (%"unix::linux_like::linux::fsid_t"*), bitcast (i64 (%"unix::pollfd"*)* @"_ZN57_$LT$libc..unix..pollfd$u20$as$u20$core..clone..Clone$GT$5clone17h6bd9157dbaad32a4E" to i64 (%"unix::linux_like::linux::fsid_t"*)*)
@"_ZN81_$LT$libc..unix..linux_like..linux..packet_mreq$u20$as$u20$core..clone..Clone$GT$5clone17h277d89c45a83e114E" = unnamed_addr alias i128 (%"unix::linux_like::linux::packet_mreq"*), bitcast (i128 (%"unix::linux_like::linux::gnu::b64::x86_64::_libc_xmmreg"*)* @"_ZN100_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64.._libc_xmmreg$u20$as$u20$core..clone..Clone$GT$5clone17h29a5cf18b565b38fE" to i128 (%"unix::linux_like::linux::packet_mreq"*)*)
@"_ZN78_$LT$libc..unix..linux_like..linux..input_id$u20$as$u20$core..clone..Clone$GT$5clone17hcc1a8030f712621bE" = unnamed_addr alias i64 (%"unix::linux_like::linux::input_id"*), bitcast (i64 (%"unix::winsize"*)* @"_ZN58_$LT$libc..unix..winsize$u20$as$u20$core..clone..Clone$GT$5clone17hb71a111626587a88E" to i64 (%"unix::linux_like::linux::input_id"*)*)
@"_ZN81_$LT$libc..unix..linux_like..linux..ff_envelope$u20$as$u20$core..clone..Clone$GT$5clone17hb7af514912547d0aE" = unnamed_addr alias i64 (%"unix::linux_like::linux::ff_envelope"*), bitcast (i64 (%"unix::winsize"*)* @"_ZN58_$LT$libc..unix..winsize$u20$as$u20$core..clone..Clone$GT$5clone17hb71a111626587a88E" to i64 (%"unix::linux_like::linux::ff_envelope"*)*)
@"_ZN89_$LT$libc..unix..linux_like..linux..ff_condition_effect$u20$as$u20$core..clone..Clone$GT$5clone17h67173d672c33acf9E" = unnamed_addr alias i96 (%"unix::linux_like::linux::ff_condition_effect"*), bitcast (i96 (%"unix::linux_like::linux::ff_ramp_effect"*)* @"_ZN84_$LT$libc..unix..linux_like..linux..ff_ramp_effect$u20$as$u20$core..clone..Clone$GT$5clone17heea59da34843e8daE" to i96 (%"unix::linux_like::linux::ff_condition_effect"*)*)
@"_ZN85_$LT$libc..unix..linux_like..linux..uinput_ff_erase$u20$as$u20$core..clone..Clone$GT$5clone17h12681173245260daE" = unnamed_addr alias i96 (%"unix::linux_like::linux::uinput_ff_erase"*), bitcast (i96 (%"unix::linux_like::in_pktinfo"*)* @"_ZN73_$LT$libc..unix..linux_like..in_pktinfo$u20$as$u20$core..clone..Clone$GT$5clone17ha5fe88908abbcc3bE" to i96 (%"unix::linux_like::linux::uinput_ff_erase"*)*)
@"_ZN79_$LT$libc..unix..linux_like..linux..Elf32_Sym$u20$as$u20$core..clone..Clone$GT$5clone17he540d7391d0b247bE" = unnamed_addr alias i128 (%"unix::linux_like::linux::Elf32_Sym"*), bitcast (i128 (%"unix::linux_like::linux::gnu::b64::x86_64::_libc_xmmreg"*)* @"_ZN100_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64.._libc_xmmreg$u20$as$u20$core..clone..Clone$GT$5clone17h29a5cf18b565b38fE" to i128 (%"unix::linux_like::linux::Elf32_Sym"*)*)
@"_ZN75_$LT$libc..unix..linux_like..linux..ucred$u20$as$u20$core..clone..Clone$GT$5clone17h4c611b182cdf236aE" = unnamed_addr alias i96 (%"unix::linux_like::linux::ucred"*), bitcast (i96 (%"unix::linux_like::in_pktinfo"*)* @"_ZN73_$LT$libc..unix..linux_like..in_pktinfo$u20$as$u20$core..clone..Clone$GT$5clone17ha5fe88908abbcc3bE" to i96 (%"unix::linux_like::linux::ucred"*)*)
@"_ZN83_$LT$libc..unix..linux_like..linux..inotify_event$u20$as$u20$core..clone..Clone$GT$5clone17h73143a4a634478f7E" = unnamed_addr alias i128 (%"unix::linux_like::linux::inotify_event"*), bitcast (i128 (%"unix::linux_like::linux::gnu::b64::x86_64::_libc_xmmreg"*)* @"_ZN100_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64.._libc_xmmreg$u20$as$u20$core..clone..Clone$GT$5clone17h29a5cf18b565b38fE" to i128 (%"unix::linux_like::linux::inotify_event"*)*)
@"_ZN81_$LT$libc..unix..linux_like..linux..sockaddr_vm$u20$as$u20$core..clone..Clone$GT$5clone17h7100400f638f467dE" = unnamed_addr alias i128 (%"unix::linux_like::linux::sockaddr_vm"*), bitcast (i128 (%"unix::linux_like::linux::gnu::b64::x86_64::_libc_xmmreg"*)* @"_ZN100_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64.._libc_xmmreg$u20$as$u20$core..clone..Clone$GT$5clone17h29a5cf18b565b38fE" to i128 (%"unix::linux_like::linux::sockaddr_vm"*)*)
@"_ZN87_$LT$libc..unix..linux_like..linux..sock_extended_err$u20$as$u20$core..clone..Clone$GT$5clone17hd41899d9f831126dE" = unnamed_addr alias i128 (%"unix::linux_like::linux::sock_extended_err"*), bitcast (i128 (%"unix::linux_like::linux::gnu::b64::x86_64::_libc_xmmreg"*)* @"_ZN100_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64.._libc_xmmreg$u20$as$u20$core..clone..Clone$GT$5clone17h29a5cf18b565b38fE" to i128 (%"unix::linux_like::linux::sock_extended_err"*)*)
@"_ZN80_$LT$libc..unix..linux_like..linux..input_mask$u20$as$u20$core..clone..Clone$GT$5clone17hf26d97435de0c168E" = unnamed_addr alias i128 (%"unix::linux_like::linux::input_mask"*), bitcast (i128 (%"unix::linux_like::linux::__c_anonymous_sockaddr_can_j1939"*)* @"_ZN102_$LT$libc..unix..linux_like..linux..__c_anonymous_sockaddr_can_j1939$u20$as$u20$core..clone..Clone$GT$5clone17h141014a64733d813E" to i128 (%"unix::linux_like::linux::input_mask"*)*)
@"_ZN81_$LT$libc..unix..linux_like..linux..sock_filter$u20$as$u20$core..clone..Clone$GT$5clone17hde4048baab9eaa61E" = unnamed_addr alias i64 (%"unix::linux_like::linux::sock_filter"*), bitcast (i64 (%"unix::pollfd"*)* @"_ZN57_$LT$libc..unix..pollfd$u20$as$u20$core..clone..Clone$GT$5clone17h6bd9157dbaad32a4E" to i64 (%"unix::linux_like::linux::sock_filter"*)*)
@"_ZN78_$LT$libc..unix..linux_like..linux..nlmsghdr$u20$as$u20$core..clone..Clone$GT$5clone17hcbe3e63fe81d5b51E" = unnamed_addr alias i128 (%"unix::linux_like::linux::nlmsghdr"*), bitcast (i128 (%"unix::linux_like::linux::gnu::b64::x86_64::_libc_xmmreg"*)* @"_ZN100_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64.._libc_xmmreg$u20$as$u20$core..clone..Clone$GT$5clone17h29a5cf18b565b38fE" to i128 (%"unix::linux_like::linux::nlmsghdr"*)*)
@"_ZN81_$LT$libc..unix..linux_like..linux..sockaddr_nl$u20$as$u20$core..clone..Clone$GT$5clone17h6f4adffa02bbe2abE" = unnamed_addr alias i96 (%"unix::linux_like::linux::sockaddr_nl"*), bitcast (i96 (%"unix::linux_like::in_pktinfo"*)* @"_ZN73_$LT$libc..unix..linux_like..in_pktinfo$u20$as$u20$core..clone..Clone$GT$5clone17ha5fe88908abbcc3bE" to i96 (%"unix::linux_like::linux::sockaddr_nl"*)*)
@"_ZN105_$LT$libc..unix..linux_like..linux..__c_anonymous_sockaddr_can_can_addr$u20$as$u20$core..clone..Clone$GT$5clone17h451e7044b6f1e503E" = unnamed_addr alias i128 (%"unix::linux_like::linux::__c_anonymous_sockaddr_can_can_addr"*), bitcast (i128 (%"unix::linux_like::linux::__c_anonymous_sockaddr_can_j1939"*)* @"_ZN102_$LT$libc..unix..linux_like..linux..__c_anonymous_sockaddr_can_j1939$u20$as$u20$core..clone..Clone$GT$5clone17h141014a64733d813E" to i128 (%"unix::linux_like::linux::__c_anonymous_sockaddr_can_can_addr"*)*)
@"_ZN90_$LT$libc..unix..linux_like..linux..gnu..statx_timestamp$u20$as$u20$core..clone..Clone$GT$5clone17h33f82886ab5b45aeE" = unnamed_addr alias i128 (%"unix::linux_like::linux::gnu::statx_timestamp"*), bitcast (i128 (%"unix::linux_like::linux::__c_anonymous_sockaddr_can_j1939"*)* @"_ZN102_$LT$libc..unix..linux_like..linux..__c_anonymous_sockaddr_can_j1939$u20$as$u20$core..clone..Clone$GT$5clone17h141014a64733d813E" to i128 (%"unix::linux_like::linux::gnu::statx_timestamp"*)*)
@"_ZN82_$LT$libc..unix..linux_like..linux..gnu..cmsghdr$u20$as$u20$core..clone..Clone$GT$5clone17hb2bd06c9a515275aE" = unnamed_addr alias i128 (%"unix::linux_like::linux::gnu::cmsghdr"*), bitcast (i128 (%"unix::linux_like::linux::__c_anonymous_sockaddr_can_j1939"*)* @"_ZN102_$LT$libc..unix..linux_like..linux..__c_anonymous_sockaddr_can_j1939$u20$as$u20$core..clone..Clone$GT$5clone17h141014a64733d813E" to i128 (%"unix::linux_like::linux::gnu::cmsghdr"*)*)
@"_ZN86_$LT$libc..unix..linux_like..linux..gnu..nl_mmap_req$u20$as$u20$core..clone..Clone$GT$5clone17h3ec508eb67b7aba5E" = unnamed_addr alias i128 (%"unix::linux_like::linux::gnu::nl_mmap_req"*), bitcast (i128 (%"unix::linux_like::linux::gnu::b64::x86_64::_libc_xmmreg"*)* @"_ZN100_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64.._libc_xmmreg$u20$as$u20$core..clone..Clone$GT$5clone17h29a5cf18b565b38fE" to i128 (%"unix::linux_like::linux::gnu::nl_mmap_req"*)*)
@"_ZN85_$LT$libc..unix..linux_like..linux..gnu..Elf32_Chdr$u20$as$u20$core..clone..Clone$GT$5clone17h09f96cd809ece864E" = unnamed_addr alias i96 (%"unix::linux_like::linux::gnu::Elf32_Chdr"*), bitcast (i96 (%"unix::linux_like::in_pktinfo"*)* @"_ZN73_$LT$libc..unix..linux_like..in_pktinfo$u20$as$u20$core..clone..Clone$GT$5clone17ha5fe88908abbcc3bE" to i96 (%"unix::linux_like::linux::gnu::Elf32_Chdr"*)*)
@"_ZN71_$LT$libc..unix..linux_like..sockaddr$u20$as$u20$core..clone..Clone$GT$5clone17hd6df9550e65ece76E" = unnamed_addr alias i128 (%"unix::linux_like::sockaddr"*), bitcast (i128 (%"unix::linux_like::linux::gnu::b64::x86_64::_libc_fpxreg"*)* @"_ZN100_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64.._libc_fpxreg$u20$as$u20$core..clone..Clone$GT$5clone17h0713ba9d1aff421bE" to i128 (%"unix::linux_like::sockaddr"*)*)
@"_ZN74_$LT$libc..unix..linux_like..sockaddr_in$u20$as$u20$core..clone..Clone$GT$5clone17h5870857cebe75716E" = unnamed_addr alias i128 (%"unix::linux_like::sockaddr_in"*), bitcast (i128 (%"unix::linux_like::linux::gnu::b64::x86_64::_libc_xmmreg"*)* @"_ZN100_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64.._libc_xmmreg$u20$as$u20$core..clone..Clone$GT$5clone17h29a5cf18b565b38fE" to i128 (%"unix::linux_like::sockaddr_in"*)*)
@"_ZN96_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..ip_mreqn$u20$as$u20$core..clone..Clone$GT$5clone17h749e51c181b08d0dE" = unnamed_addr alias i96 (%"unix::linux_like::linux::gnu::b64::x86_64::ip_mreqn"*), bitcast (i96 (%"unix::linux_like::in_pktinfo"*)* @"_ZN73_$LT$libc..unix..linux_like..in_pktinfo$u20$as$u20$core..clone..Clone$GT$5clone17ha5fe88908abbcc3bE" to i96 (%"unix::linux_like::linux::gnu::b64::x86_64::ip_mreqn"*)*)
@"_ZN90_$LT$libc..unix..linux_like..linux..pthread_rwlockattr_t$u20$as$u20$core..clone..Clone$GT$5clone17h62c177e0ea5aa148E" = unnamed_addr alias i64 (%"unix::linux_like::linux::pthread_rwlockattr_t"*), bitcast (i64 (%"unix::sigval"*)* @"_ZN57_$LT$libc..unix..sigval$u20$as$u20$core..clone..Clone$GT$5clone17h22a20db92647a3cdE" to i64 (%"unix::linux_like::linux::pthread_rwlockattr_t"*)*)
@"_ZN89_$LT$libc..unix..linux_like..linux..pthread_mutexattr_t$u20$as$u20$core..clone..Clone$GT$5clone17h94141387227940fcE" = unnamed_addr alias i32 (%"unix::linux_like::linux::pthread_mutexattr_t"*), bitcast (i32 (%"unix::linux_like::linux::pthread_condattr_t"*)* @"_ZN88_$LT$libc..unix..linux_like..linux..pthread_condattr_t$u20$as$u20$core..clone..Clone$GT$5clone17h83d2a4843a78293eE" to i32 (%"unix::linux_like::linux::pthread_mutexattr_t"*)*)
@"_ZN79_$LT$libc..unix..linux_like..linux..can_frame$u20$as$u20$core..clone..Clone$GT$5clone17hb0769f69a26c8124E" = unnamed_addr alias i128 (%"unix::linux_like::linux::can_frame"*), bitcast (i128 (%"unix::linux_like::linux::__c_anonymous_sockaddr_can_j1939"*)* @"_ZN102_$LT$libc..unix..linux_like..linux..__c_anonymous_sockaddr_can_j1939$u20$as$u20$core..clone..Clone$GT$5clone17h141014a64733d813E" to i128 (%"unix::linux_like::linux::can_frame"*)*)
@"_ZN66_$LT$libc..unix..align..in6_addr$u20$as$u20$core..clone..Clone$GT$5clone17h89d32e1ac9be6d94E" = unnamed_addr alias i128 (%"unix::align::in6_addr"*), bitcast (i128 (%"unix::linux_like::linux::gnu::b64::x86_64::_libc_xmmreg"*)* @"_ZN100_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64.._libc_xmmreg$u20$as$u20$core..clone..Clone$GT$5clone17h29a5cf18b565b38fE" to i128 (%"unix::align::in6_addr"*)*)
@"_ZN74_$LT$libc..unix..linux_like..sched_param$u20$as$u20$core..clone..Clone$GT$5clone17h3fd46decb7fdb87eE" = unnamed_addr alias i32 (%"unix::linux_like::sched_param"*), bitcast (i32 (%"unix::linux_like::in_addr"*)* @"_ZN70_$LT$libc..unix..linux_like..in_addr$u20$as$u20$core..clone..Clone$GT$5clone17h6b715240fc3acb56E" to i32 (%"unix::linux_like::sched_param"*)*)
@"_ZN79_$LT$libc..unix..linux_like..linux..af_alg_iv$u20$as$u20$core..clone..Clone$GT$5clone17h5ccd093b4cedef31E" = unnamed_addr alias i32 (%"unix::linux_like::linux::af_alg_iv"*), bitcast (i32 (%"unix::linux_like::in_addr"*)* @"_ZN70_$LT$libc..unix..linux_like..in_addr$u20$as$u20$core..clone..Clone$GT$5clone17h6b715240fc3acb56E" to i32 (%"unix::linux_like::linux::af_alg_iv"*)*)
@"_ZN85_$LT$libc..unix..linux_like..linux..gnu..nl_pktinfo$u20$as$u20$core..clone..Clone$GT$5clone17h03104a9790e0aeccE" = unnamed_addr alias i32 (%"unix::linux_like::linux::gnu::nl_pktinfo"*), bitcast (i32 (%"unix::linux_like::in_addr"*)* @"_ZN70_$LT$libc..unix..linux_like..in_addr$u20$as$u20$core..clone..Clone$GT$5clone17h6b715240fc3acb56E" to i32 (%"unix::linux_like::linux::gnu::nl_pktinfo"*)*)
@"_ZN58_$LT$libc..unix..servent$u20$as$u20$core..clone..Clone$GT$5clone17h85b013f751caa1daE" = unnamed_addr alias void (%"unix::servent"*, %"unix::servent"*), bitcast (void (%"unix::group"*, %"unix::group"*)* @"_ZN56_$LT$libc..unix..group$u20$as$u20$core..clone..Clone$GT$5clone17h82461ecd83cca4bfE" to void (%"unix::servent"*, %"unix::servent"*)*)
@"_ZN70_$LT$libc..unix..linux_like..Dl_info$u20$as$u20$core..clone..Clone$GT$5clone17h5cdf251a4c281d87E" = unnamed_addr alias void (%"unix::linux_like::Dl_info"*, %"unix::linux_like::Dl_info"*), bitcast (void (%"unix::tms"*, %"unix::tms"*)* @"_ZN54_$LT$libc..unix..tms$u20$as$u20$core..clone..Clone$GT$5clone17he2435542381a4158E" to void (%"unix::linux_like::Dl_info"*, %"unix::linux_like::Dl_info"*)*)
@"_ZN80_$LT$libc..unix..linux_like..linux..itimerspec$u20$as$u20$core..clone..Clone$GT$5clone17h22ae020ba063e42dE" = unnamed_addr alias void (%"unix::linux_like::linux::itimerspec"*, %"unix::linux_like::linux::itimerspec"*), bitcast (void (%"unix::itimerval"*, %"unix::itimerval"*)* @"_ZN60_$LT$libc..unix..itimerval$u20$as$u20$core..clone..Clone$GT$5clone17h6142c8434e217285E" to void (%"unix::linux_like::linux::itimerspec"*, %"unix::linux_like::linux::itimerspec"*)*)
@"_ZN79_$LT$libc..unix..linux_like..linux..cpu_set_t$u20$as$u20$core..clone..Clone$GT$5clone17h91ba269b5d708609E" = unnamed_addr alias void (%"unix::linux_like::linux::cpu_set_t"*, %"unix::linux_like::linux::cpu_set_t"*), bitcast (void (%"unix::linux_like::fd_set"*, %"unix::linux_like::fd_set"*)* @"_ZN69_$LT$libc..unix..linux_like..fd_set$u20$as$u20$core..clone..Clone$GT$5clone17h3857db7446687e89E" to void (%"unix::linux_like::linux::cpu_set_t"*, %"unix::linux_like::linux::cpu_set_t"*)*)
@"_ZN78_$LT$libc..unix..linux_like..linux..dirent64$u20$as$u20$core..clone..Clone$GT$5clone17h21b74ca86d428c8eE" = unnamed_addr alias void (%"unix::linux_like::linux::dirent64"*, %"unix::linux_like::linux::dirent64"*), bitcast (void (%"unix::linux_like::linux::dirent"*, %"unix::linux_like::linux::dirent"*)* @"_ZN76_$LT$libc..unix..linux_like..linux..dirent$u20$as$u20$core..clone..Clone$GT$5clone17h28b4931108e6f334E" to void (%"unix::linux_like::linux::dirent64"*, %"unix::linux_like::linux::dirent64"*)*)
@"_ZN83_$LT$libc..unix..linux_like..linux..gnu..glob64_t$u20$as$u20$core..clone..Clone$GT$5clone17hbb47f9a11adf61b4E" = unnamed_addr alias void (%"unix::linux_like::linux::gnu::glob64_t"*, %"unix::linux_like::linux::gnu::glob64_t"*), bitcast (void (%"unix::linux_like::linux::glob_t"*, %"unix::linux_like::linux::glob_t"*)* @"_ZN76_$LT$libc..unix..linux_like..linux..glob_t$u20$as$u20$core..clone..Clone$GT$5clone17h8e1ae773a5d987c2E" to void (%"unix::linux_like::linux::gnu::glob64_t"*, %"unix::linux_like::linux::gnu::glob64_t"*)*)
@"_ZN83_$LT$libc..unix..linux_like..linux..gnu..mallinfo$u20$as$u20$core..clone..Clone$GT$5clone17h5a735f4dcaaf8a79E" = unnamed_addr alias void (%"unix::linux_like::linux::gnu::mallinfo"*, %"unix::linux_like::linux::gnu::mallinfo"*), bitcast (void (%"unix::linux_like::linux::Elf32_Shdr"*, %"unix::linux_like::linux::Elf32_Shdr"*)* @"_ZN80_$LT$libc..unix..linux_like..linux..Elf32_Shdr$u20$as$u20$core..clone..Clone$GT$5clone17hb0030e1715d5404eE" to void (%"unix::linux_like::linux::gnu::mallinfo"*, %"unix::linux_like::linux::gnu::mallinfo"*)*)
@"_ZN86_$LT$libc..unix..linux_like..linux..gnu..nl_mmap_hdr$u20$as$u20$core..clone..Clone$GT$5clone17hb4d8f8af7178e45fE" = unnamed_addr alias void (%"unix::linux_like::linux::gnu::nl_mmap_hdr"*, %"unix::linux_like::linux::gnu::nl_mmap_hdr"*), bitcast (void (%"unix::linux_like::linux::input_absinfo"*, %"unix::linux_like::linux::input_absinfo"*)* @"_ZN83_$LT$libc..unix..linux_like..linux..input_absinfo$u20$as$u20$core..clone..Clone$GT$5clone17h13a698fd16e6a92dE" to void (%"unix::linux_like::linux::gnu::nl_mmap_hdr"*, %"unix::linux_like::linux::gnu::nl_mmap_hdr"*)*)
@"_ZN82_$LT$libc..unix..linux_like..linux..gnu..seminfo$u20$as$u20$core..clone..Clone$GT$5clone17h2e3385baea7d7cd4E" = unnamed_addr alias void (%"unix::linux_like::linux::gnu::seminfo"*, %"unix::linux_like::linux::gnu::seminfo"*), bitcast (void (%"unix::linux_like::linux::Elf32_Shdr"*, %"unix::linux_like::linux::Elf32_Shdr"*)* @"_ZN80_$LT$libc..unix..linux_like..linux..Elf32_Shdr$u20$as$u20$core..clone..Clone$GT$5clone17hb0030e1715d5404eE" to void (%"unix::linux_like::linux::gnu::seminfo"*, %"unix::linux_like::linux::gnu::seminfo"*)*)
@"_ZN88_$LT$libc..unix..linux_like..linux..gnu..b64..sigset_t$u20$as$u20$core..clone..Clone$GT$5clone17ha97d3fd3b08257eaE" = unnamed_addr alias void (%"unix::linux_like::linux::gnu::b64::sigset_t"*, %"unix::linux_like::linux::gnu::b64::sigset_t"*), bitcast (void (%"unix::linux_like::fd_set"*, %"unix::linux_like::fd_set"*)* @"_ZN69_$LT$libc..unix..linux_like..fd_set$u20$as$u20$core..clone..Clone$GT$5clone17h3857db7446687e89E" to void (%"unix::linux_like::linux::gnu::b64::sigset_t"*, %"unix::linux_like::linux::gnu::b64::sigset_t"*)*)
@"_ZN95_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..flock64$u20$as$u20$core..clone..Clone$GT$5clone17h76a0cf0099cd3b59E" = unnamed_addr alias void (%"unix::linux_like::linux::gnu::b64::x86_64::flock64"*, %"unix::linux_like::linux::gnu::b64::x86_64::flock64"*), bitcast (void (%"unix::linux_like::linux::gnu::b64::x86_64::flock"*, %"unix::linux_like::linux::gnu::b64::x86_64::flock"*)* @"_ZN93_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..flock$u20$as$u20$core..clone..Clone$GT$5clone17h3425a14370c6df63E" to void (%"unix::linux_like::linux::gnu::b64::x86_64::flock64"*, %"unix::linux_like::linux::gnu::b64::x86_64::flock64"*)*)
@"_ZN94_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..stat64$u20$as$u20$core..clone..Clone$GT$5clone17h77ab71d79562feabE" = unnamed_addr alias void (%"unix::linux_like::linux::gnu::b64::x86_64::stat64"*, %"unix::linux_like::linux::gnu::b64::x86_64::stat64"*), bitcast (void (%"unix::linux_like::linux::gnu::b64::x86_64::stat"*, %"unix::linux_like::linux::gnu::b64::x86_64::stat"*)* @"_ZN92_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..stat$u20$as$u20$core..clone..Clone$GT$5clone17h3bd98f2fcfa01778E" to void (%"unix::linux_like::linux::gnu::b64::x86_64::stat64"*, %"unix::linux_like::linux::gnu::b64::x86_64::stat64"*)*)
@"_ZN97_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..statvfs64$u20$as$u20$core..clone..Clone$GT$5clone17h964d380b8773724eE" = unnamed_addr alias void (%"unix::linux_like::linux::gnu::b64::x86_64::statvfs64"*, %"unix::linux_like::linux::gnu::b64::x86_64::statvfs64"*), bitcast (void (%"unix::linux_like::linux::gnu::b64::x86_64::not_x32::statvfs"*, %"unix::linux_like::linux::gnu::b64::x86_64::not_x32::statvfs"*)* @"_ZN104_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..not_x32..statvfs$u20$as$u20$core..clone..Clone$GT$5clone17ha213771763e1f6a5E" to void (%"unix::linux_like::linux::gnu::b64::x86_64::statvfs64"*, %"unix::linux_like::linux::gnu::b64::x86_64::statvfs64"*)*)
@"_ZN55_$LT$libc..unix..FILE$u20$as$u20$core..clone..Clone$GT$5clone17hf0b01be7d961899fE" = unnamed_addr alias void (%"unix::FILE"*), bitcast (void (%"unix::DIR"*)* @"_ZN54_$LT$libc..unix..DIR$u20$as$u20$core..clone..Clone$GT$5clone17h8d8eb5cb60cbf503E" to void (%"unix::FILE"*)*)
@"_ZN57_$LT$libc..unix..fpos_t$u20$as$u20$core..clone..Clone$GT$5clone17h24c3ab29d5d2d600E" = unnamed_addr alias void (%"unix::fpos_t"*), bitcast (void (%"unix::DIR"*)* @"_ZN54_$LT$libc..unix..DIR$u20$as$u20$core..clone..Clone$GT$5clone17h8d8eb5cb60cbf503E" to void (%"unix::fpos_t"*)*)
@"_ZN71_$LT$libc..unix..linux_like..timezone$u20$as$u20$core..clone..Clone$GT$5clone17h9d3925036933cc71E" = unnamed_addr alias void (%"unix::linux_like::timezone"*), bitcast (void (%"unix::DIR"*)* @"_ZN54_$LT$libc..unix..DIR$u20$as$u20$core..clone..Clone$GT$5clone17h8d8eb5cb60cbf503E" to void (%"unix::linux_like::timezone"*)*)
@"_ZN78_$LT$libc..unix..linux_like..linux..fpos64_t$u20$as$u20$core..clone..Clone$GT$5clone17h654790a7a352d306E" = unnamed_addr alias void (%"unix::linux_like::linux::fpos64_t"*), bitcast (void (%"unix::DIR"*)* @"_ZN54_$LT$libc..unix..DIR$u20$as$u20$core..clone..Clone$GT$5clone17h8d8eb5cb60cbf503E" to void (%"unix::linux_like::linux::fpos64_t"*)*)
@"_ZN58_$LT$libc..unix..utimbuf$u20$as$u20$core..clone..Clone$GT$5clone17h0a11dfb79b41207aE" = unnamed_addr alias { i64, i64 } ({ i64, i64 }*), bitcast ({ i8*, i64 } ({ i8*, i64 }*)* @"_ZN56_$LT$libc..unix..iovec$u20$as$u20$core..clone..Clone$GT$5clone17h64061571f6e79a69E" to { i64, i64 } ({ i64, i64 }*)*)
@"_ZN59_$LT$libc..unix..timespec$u20$as$u20$core..clone..Clone$GT$5clone17h7a528ef86d4cfad0E" = unnamed_addr alias { i64, i64 } ({ i64, i64 }*), bitcast ({ i8*, i64 } ({ i8*, i64 }*)* @"_ZN56_$LT$libc..unix..iovec$u20$as$u20$core..clone..Clone$GT$5clone17h64061571f6e79a69E" to { i64, i64 } ({ i64, i64 }*)*)
@"_ZN58_$LT$libc..unix..timeval$u20$as$u20$core..clone..Clone$GT$5clone17hb1205cc7c506f4edE" = unnamed_addr alias { i64, i64 } ({ i64, i64 }*), bitcast ({ i8*, i64 } ({ i8*, i64 }*)* @"_ZN56_$LT$libc..unix..iovec$u20$as$u20$core..clone..Clone$GT$5clone17h64061571f6e79a69E" to { i64, i64 } ({ i64, i64 }*)*)
@"_ZN57_$LT$libc..unix..rlimit$u20$as$u20$core..clone..Clone$GT$5clone17hda3ba08f6baa98a9E" = unnamed_addr alias { i64, i64 } ({ i64, i64 }*), bitcast ({ i8*, i64 } ({ i8*, i64 }*)* @"_ZN56_$LT$libc..unix..iovec$u20$as$u20$core..clone..Clone$GT$5clone17h64061571f6e79a69E" to { i64, i64 } ({ i64, i64 }*)*)
@"_ZN78_$LT$libc..unix..linux_like..linux..rlimit64$u20$as$u20$core..clone..Clone$GT$5clone17h737f81d1a6e4be0cE" = unnamed_addr alias { i64, i64 } ({ i64, i64 }*), bitcast ({ i8*, i64 } ({ i8*, i64 }*)* @"_ZN56_$LT$libc..unix..iovec$u20$as$u20$core..clone..Clone$GT$5clone17h64061571f6e79a69E" to { i64, i64 } ({ i64, i64 }*)*)
@"_ZN80_$LT$libc..unix..linux_like..linux..ff_trigger$u20$as$u20$core..clone..Clone$GT$5clone17hf2e8b78c27f4a4d2E" = unnamed_addr alias { i16, i16 } ({ i16, i16 }*), { i16, i16 } ({ i16, i16 }*)* @"_ZN76_$LT$libc..unix..linux_like..linux..nlattr$u20$as$u20$core..clone..Clone$GT$5clone17hd4f52d91c333441fE"
@"_ZN86_$LT$libc..unix..linux_like..linux..ff_rumble_effect$u20$as$u20$core..clone..Clone$GT$5clone17h42ab6141238a5fc3E" = unnamed_addr alias { i16, i16 } ({ i16, i16 }*), { i16, i16 } ({ i16, i16 }*)* @"_ZN76_$LT$libc..unix..linux_like..linux..nlattr$u20$as$u20$core..clone..Clone$GT$5clone17hd4f52d91c333441fE"
@"_ZN87_$LT$libc..unix..linux_like..linux..fanotify_response$u20$as$u20$core..clone..Clone$GT$5clone17hc66ef4e7c04e4776E" = unnamed_addr alias { i32, i32 } ({ i32, i32 }*), { i32, i32 } ({ i32, i32 }*)* @"_ZN57_$LT$libc..unix..linger$u20$as$u20$core..clone..Clone$GT$5clone17h63275cca22b653b6E"
@"_ZN80_$LT$libc..unix..linux_like..linux..regmatch_t$u20$as$u20$core..clone..Clone$GT$5clone17hae5fb5ea1a53ef0bE" = unnamed_addr alias { i32, i32 } ({ i32, i32 }*), { i32, i32 } ({ i32, i32 }*)* @"_ZN57_$LT$libc..unix..linger$u20$as$u20$core..clone..Clone$GT$5clone17h63275cca22b653b6E"
@"_ZN99_$LT$libc..unix..linux_like..linux..__c_anonymous_sockaddr_can_tp$u20$as$u20$core..clone..Clone$GT$5clone17hc607f99919691071E" = unnamed_addr alias { i32, i32 } ({ i32, i32 }*), { i32, i32 } ({ i32, i32 }*)* @"_ZN57_$LT$libc..unix..linger$u20$as$u20$core..clone..Clone$GT$5clone17h63275cca22b653b6E"
@"_ZN80_$LT$libc..unix..linux_like..linux..can_filter$u20$as$u20$core..clone..Clone$GT$5clone17ha057a8462456f528E" = unnamed_addr alias { i32, i32 } ({ i32, i32 }*), { i32, i32 } ({ i32, i32 }*)* @"_ZN57_$LT$libc..unix..linger$u20$as$u20$core..clone..Clone$GT$5clone17h63275cca22b653b6E"
@"_ZN79_$LT$libc..unix..linux_like..linux..ff_replay$u20$as$u20$core..clone..Clone$GT$5clone17h0bf371c98a1c94baE" = unnamed_addr alias { i16, i16 } ({ i16, i16 }*), { i16, i16 } ({ i16, i16 }*)* @"_ZN76_$LT$libc..unix..linux_like..linux..nlattr$u20$as$u20$core..clone..Clone$GT$5clone17hd4f52d91c333441fE"
@"_ZN81_$LT$libc..unix..linux_like..linux..sock_txtime$u20$as$u20$core..clone..Clone$GT$5clone17hc959373e00f343d2E" = unnamed_addr alias { i32, i32 } ({ i32, i32 }*), { i32, i32 } ({ i32, i32 }*)* @"_ZN57_$LT$libc..unix..linger$u20$as$u20$core..clone..Clone$GT$5clone17h63275cca22b653b6E"
@"_ZN88_$LT$libc..unix..linux_like..linux..gnu..__exit_status$u20$as$u20$core..clone..Clone$GT$5clone17h9651d6849225bed8E" = unnamed_addr alias { i16, i16 } ({ i16, i16 }*), { i16, i16 } ({ i16, i16 }*)* @"_ZN76_$LT$libc..unix..linux_like..linux..nlattr$u20$as$u20$core..clone..Clone$GT$5clone17hd4f52d91c333441fE"
@"_ZN84_$LT$libc..unix..linux_like..linux..gnu..__timeval$u20$as$u20$core..clone..Clone$GT$5clone17hda1ee0ddda6ce31dE" = unnamed_addr alias { i32, i32 } ({ i32, i32 }*), { i32, i32 } ({ i32, i32 }*)* @"_ZN57_$LT$libc..unix..linger$u20$as$u20$core..clone..Clone$GT$5clone17h63275cca22b653b6E"

; <libc::unix::DIR as core::clone::Clone>::clone
; Function Attrs: noreturn nounwind nonlazybind uwtable
define void @"_ZN54_$LT$libc..unix..DIR$u20$as$u20$core..clone..Clone$GT$5clone17h8d8eb5cb60cbf503E"(%"unix::DIR"* noalias nocapture nonnull readonly align 1 %self) unnamed_addr #0 {
start:
  tail call void @llvm.trap()
  unreachable
}

; <libc::unix::group as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN56_$LT$libc..unix..group$u20$as$u20$core..clone..Clone$GT$5clone17h82461ecd83cca4bfE"(%"unix::group"* noalias nocapture sret(%"unix::group") dereferenceable(32) %0, %"unix::group"* noalias nocapture readonly align 8 dereferenceable(32) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::group"* %0 to i8*
  %2 = bitcast %"unix::group"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(32) %1, i8* noundef nonnull align 8 dereferenceable(32) %2, i64 32, i1 false)
  ret void
}

; <libc::unix::rusage as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN57_$LT$libc..unix..rusage$u20$as$u20$core..clone..Clone$GT$5clone17hc1bd1225fc29acf1E"(%"unix::rusage"* noalias nocapture sret(%"unix::rusage") dereferenceable(144) %0, %"unix::rusage"* noalias nocapture readonly align 8 dereferenceable(144) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::rusage"* %0 to i8*
  %2 = bitcast %"unix::rusage"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(144) %1, i8* noundef nonnull align 8 dereferenceable(144) %2, i64 144, i1 false)
  ret void
}

; <libc::unix::ipv6_mreq as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN60_$LT$libc..unix..ipv6_mreq$u20$as$u20$core..clone..Clone$GT$5clone17hbb1a5c27fc2d689fE"(%"unix::ipv6_mreq"* noalias nocapture sret(%"unix::ipv6_mreq") dereferenceable(20) %0, %"unix::ipv6_mreq"* noalias nocapture readonly align 4 dereferenceable(20) %self) unnamed_addr #1 {
start:
  %1 = getelementptr inbounds %"unix::ipv6_mreq", %"unix::ipv6_mreq"* %0, i64 0, i32 0, i32 0, i64 0
  %2 = getelementptr inbounds %"unix::ipv6_mreq", %"unix::ipv6_mreq"* %self, i64 0, i32 0, i32 0, i64 0
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 4 dereferenceable(20) %1, i8* noundef nonnull align 4 dereferenceable(20) %2, i64 20, i1 false)
  ret void
}

; <libc::unix::hostent as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN58_$LT$libc..unix..hostent$u20$as$u20$core..clone..Clone$GT$5clone17h8d5297ce99bdadf2E"(%"unix::hostent"* noalias nocapture sret(%"unix::hostent") dereferenceable(32) %0, %"unix::hostent"* noalias nocapture readonly align 8 dereferenceable(32) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::hostent"* %0 to i8*
  %2 = bitcast %"unix::hostent"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(32) %1, i8* noundef nonnull align 8 dereferenceable(32) %2, i64 32, i1 false)
  ret void
}

; <libc::unix::iovec as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define { i8*, i64 } @"_ZN56_$LT$libc..unix..iovec$u20$as$u20$core..clone..Clone$GT$5clone17h64061571f6e79a69E"({ i8*, i64 }* noalias nocapture readonly align 8 dereferenceable(16) %self) unnamed_addr #2 {
start:
  %0 = getelementptr inbounds { i8*, i64 }, { i8*, i64 }* %self, i64 0, i32 0
  %1 = load i8*, i8** %0, align 8
  %2 = getelementptr inbounds { i8*, i64 }, { i8*, i64 }* %self, i64 0, i32 1
  %3 = load i64, i64* %2, align 8
  %4 = insertvalue { i8*, i64 } undef, i8* %1, 0
  %5 = insertvalue { i8*, i64 } %4, i64 %3, 1
  ret { i8*, i64 } %5
}

; <libc::unix::pollfd as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define i64 @"_ZN57_$LT$libc..unix..pollfd$u20$as$u20$core..clone..Clone$GT$5clone17h6bd9157dbaad32a4E"(%"unix::pollfd"* noalias nocapture readonly align 4 dereferenceable(8) %self) unnamed_addr #2 {
start:
  %.sroa.0.0..sroa_cast = bitcast %"unix::pollfd"* %self to i64*
  %.sroa.0.0.copyload = load i64, i64* %.sroa.0.0..sroa_cast, align 4
  ret i64 %.sroa.0.0.copyload
}

; <libc::unix::winsize as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define i64 @"_ZN58_$LT$libc..unix..winsize$u20$as$u20$core..clone..Clone$GT$5clone17hb71a111626587a88E"(%"unix::winsize"* noalias nocapture readonly align 2 dereferenceable(8) %self) unnamed_addr #2 {
start:
  %.sroa.0.0..sroa_cast = bitcast %"unix::winsize"* %self to i64*
  %.sroa.0.0.copyload = load i64, i64* %.sroa.0.0..sroa_cast, align 2
  ret i64 %.sroa.0.0.copyload
}

; <libc::unix::linger as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define { i32, i32 } @"_ZN57_$LT$libc..unix..linger$u20$as$u20$core..clone..Clone$GT$5clone17h63275cca22b653b6E"({ i32, i32 }* noalias nocapture readonly align 4 dereferenceable(8) %self) unnamed_addr #2 {
start:
  %0 = getelementptr inbounds { i32, i32 }, { i32, i32 }* %self, i64 0, i32 0
  %1 = load i32, i32* %0, align 4
  %2 = getelementptr inbounds { i32, i32 }, { i32, i32 }* %self, i64 0, i32 1
  %3 = load i32, i32* %2, align 4
  %4 = insertvalue { i32, i32 } undef, i32 %1, 0
  %5 = insertvalue { i32, i32 } %4, i32 %3, 1
  ret { i32, i32 } %5
}

; <libc::unix::sigval as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define i64 @"_ZN57_$LT$libc..unix..sigval$u20$as$u20$core..clone..Clone$GT$5clone17h22a20db92647a3cdE"(%"unix::sigval"* noalias nocapture readonly align 8 dereferenceable(8) %self) unnamed_addr #2 {
start:
  %.sroa.0.0..sroa_cast = bitcast %"unix::sigval"* %self to i64*
  %.sroa.0.0.copyload = load i64, i64* %.sroa.0.0..sroa_cast, align 8
  ret i64 %.sroa.0.0.copyload
}

; <libc::unix::itimerval as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN60_$LT$libc..unix..itimerval$u20$as$u20$core..clone..Clone$GT$5clone17h6142c8434e217285E"(%"unix::itimerval"* noalias nocapture sret(%"unix::itimerval") dereferenceable(32) %0, %"unix::itimerval"* noalias nocapture readonly align 8 dereferenceable(32) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::itimerval"* %0 to i8*
  %2 = bitcast %"unix::itimerval"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(32) %1, i8* noundef nonnull align 8 dereferenceable(32) %2, i64 32, i1 false)
  ret void
}

; <libc::unix::tms as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN54_$LT$libc..unix..tms$u20$as$u20$core..clone..Clone$GT$5clone17he2435542381a4158E"(%"unix::tms"* noalias nocapture sret(%"unix::tms") dereferenceable(32) %0, %"unix::tms"* noalias nocapture readonly align 8 dereferenceable(32) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::tms"* %0 to i8*
  %2 = bitcast %"unix::tms"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(32) %1, i8* noundef nonnull align 8 dereferenceable(32) %2, i64 32, i1 false)
  ret void
}

; <libc::unix::protoent as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN59_$LT$libc..unix..protoent$u20$as$u20$core..clone..Clone$GT$5clone17he2a7c2b763cce7c8E"(%"unix::protoent"* noalias nocapture sret(%"unix::protoent") dereferenceable(24) %0, %"unix::protoent"* noalias nocapture readonly align 8 dereferenceable(24) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::protoent"* %0 to i8*
  %2 = bitcast %"unix::protoent"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(24) %1, i8* noundef nonnull align 8 dereferenceable(24) %2, i64 24, i1 false)
  ret void
}

; <libc::unix::linux_like::in_addr as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define i32 @"_ZN70_$LT$libc..unix..linux_like..in_addr$u20$as$u20$core..clone..Clone$GT$5clone17h6b715240fc3acb56E"(%"unix::linux_like::in_addr"* noalias nocapture readonly align 4 dereferenceable(4) %self) unnamed_addr #2 {
start:
  %.sroa.0.0..sroa_idx = getelementptr inbounds %"unix::linux_like::in_addr", %"unix::linux_like::in_addr"* %self, i64 0, i32 0
  %.sroa.0.0.copyload = load i32, i32* %.sroa.0.0..sroa_idx, align 4
  ret i32 %.sroa.0.0.copyload
}

; <libc::unix::linux_like::sockaddr_in6 as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN75_$LT$libc..unix..linux_like..sockaddr_in6$u20$as$u20$core..clone..Clone$GT$5clone17h5c0eea0f06254883E"(%"unix::linux_like::sockaddr_in6"* noalias nocapture sret(%"unix::linux_like::sockaddr_in6") dereferenceable(28) %0, %"unix::linux_like::sockaddr_in6"* noalias nocapture readonly align 4 dereferenceable(28) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::sockaddr_in6"* %0 to i8*
  %2 = bitcast %"unix::linux_like::sockaddr_in6"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 4 dereferenceable(28) %1, i8* noundef nonnull align 4 dereferenceable(28) %2, i64 28, i1 false)
  ret void
}

; <libc::unix::linux_like::addrinfo as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN71_$LT$libc..unix..linux_like..addrinfo$u20$as$u20$core..clone..Clone$GT$5clone17h38aefe645e11b46dE"(%"unix::linux_like::addrinfo"* noalias nocapture sret(%"unix::linux_like::addrinfo") dereferenceable(48) %0, %"unix::linux_like::addrinfo"* noalias nocapture readonly align 8 dereferenceable(48) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::addrinfo"* %0 to i8*
  %2 = bitcast %"unix::linux_like::addrinfo"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(48) %1, i8* noundef nonnull align 8 dereferenceable(48) %2, i64 48, i1 false)
  ret void
}

; <libc::unix::linux_like::sockaddr_ll as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN74_$LT$libc..unix..linux_like..sockaddr_ll$u20$as$u20$core..clone..Clone$GT$5clone17h91f0249c5ec654a7E"(%"unix::linux_like::sockaddr_ll"* noalias nocapture sret(%"unix::linux_like::sockaddr_ll") dereferenceable(20) %0, %"unix::linux_like::sockaddr_ll"* noalias nocapture readonly align 4 dereferenceable(20) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::sockaddr_ll"* %0 to i8*
  %2 = bitcast %"unix::linux_like::sockaddr_ll"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 4 dereferenceable(20) %1, i8* noundef nonnull align 4 dereferenceable(20) %2, i64 20, i1 false)
  ret void
}

; <libc::unix::linux_like::fd_set as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN69_$LT$libc..unix..linux_like..fd_set$u20$as$u20$core..clone..Clone$GT$5clone17h3857db7446687e89E"(%"unix::linux_like::fd_set"* noalias nocapture sret(%"unix::linux_like::fd_set") dereferenceable(128) %0, %"unix::linux_like::fd_set"* noalias nocapture readonly align 8 dereferenceable(128) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::fd_set"* %0 to i8*
  %2 = bitcast %"unix::linux_like::fd_set"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(128) %1, i8* noundef nonnull align 8 dereferenceable(128) %2, i64 128, i1 false)
  ret void
}

; <libc::unix::linux_like::tm as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN65_$LT$libc..unix..linux_like..tm$u20$as$u20$core..clone..Clone$GT$5clone17h39975076f8fab1e4E"(%"unix::linux_like::tm"* noalias nocapture sret(%"unix::linux_like::tm") dereferenceable(56) %0, %"unix::linux_like::tm"* noalias nocapture readonly align 8 dereferenceable(56) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::tm"* %0 to i8*
  %2 = bitcast %"unix::linux_like::tm"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(56) %1, i8* noundef nonnull align 8 dereferenceable(56) %2, i64 56, i1 false)
  ret void
}

; <libc::unix::linux_like::lconv as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN68_$LT$libc..unix..linux_like..lconv$u20$as$u20$core..clone..Clone$GT$5clone17ha5eacd62f05b8403E"(%"unix::linux_like::lconv"* noalias nocapture sret(%"unix::linux_like::lconv") dereferenceable(96) %0, %"unix::linux_like::lconv"* noalias nocapture readonly align 8 dereferenceable(96) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::lconv"* %0 to i8*
  %2 = bitcast %"unix::linux_like::lconv"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(96) %1, i8* noundef nonnull align 8 dereferenceable(96) %2, i64 96, i1 false)
  ret void
}

; <libc::unix::linux_like::in_pktinfo as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define i96 @"_ZN73_$LT$libc..unix..linux_like..in_pktinfo$u20$as$u20$core..clone..Clone$GT$5clone17ha5fe88908abbcc3bE"(%"unix::linux_like::in_pktinfo"* noalias nocapture readonly align 4 dereferenceable(12) %self) unnamed_addr #2 {
start:
  %.sroa.0.0..sroa_cast = bitcast %"unix::linux_like::in_pktinfo"* %self to i96*
  %.sroa.0.0.copyload = load i96, i96* %.sroa.0.0..sroa_cast, align 4
  ret i96 %.sroa.0.0.copyload
}

; <libc::unix::linux_like::ifaddrs as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN70_$LT$libc..unix..linux_like..ifaddrs$u20$as$u20$core..clone..Clone$GT$5clone17hed000be425c03ff6E"(%"unix::linux_like::ifaddrs"* noalias nocapture sret(%"unix::linux_like::ifaddrs") dereferenceable(56) %0, %"unix::linux_like::ifaddrs"* noalias nocapture readonly align 8 dereferenceable(56) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::ifaddrs"* %0 to i8*
  %2 = bitcast %"unix::linux_like::ifaddrs"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(56) %1, i8* noundef nonnull align 8 dereferenceable(56) %2, i64 56, i1 false)
  ret void
}

; <libc::unix::linux_like::in6_rtmsg as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN72_$LT$libc..unix..linux_like..in6_rtmsg$u20$as$u20$core..clone..Clone$GT$5clone17h132899b08cca9348E"(%"unix::linux_like::in6_rtmsg"* noalias nocapture sret(%"unix::linux_like::in6_rtmsg") dereferenceable(80) %0, %"unix::linux_like::in6_rtmsg"* noalias nocapture readonly align 8 dereferenceable(80) %self) unnamed_addr #1 {
start:
  %1 = getelementptr inbounds %"unix::linux_like::in6_rtmsg", %"unix::linux_like::in6_rtmsg"* %0, i64 0, i32 0, i32 0, i64 0
  %2 = getelementptr inbounds %"unix::linux_like::in6_rtmsg", %"unix::linux_like::in6_rtmsg"* %self, i64 0, i32 0, i32 0, i64 0
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(80) %1, i8* noundef nonnull align 8 dereferenceable(80) %2, i64 80, i1 false)
  ret void
}

; <libc::unix::linux_like::arpreq as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN69_$LT$libc..unix..linux_like..arpreq$u20$as$u20$core..clone..Clone$GT$5clone17h5cbbbb7b48e9ba61E"(%"unix::linux_like::arpreq"* noalias nocapture sret(%"unix::linux_like::arpreq") dereferenceable(68) %0, %"unix::linux_like::arpreq"* noalias nocapture readonly align 4 dereferenceable(68) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::arpreq"* %0 to i8*
  %2 = bitcast %"unix::linux_like::arpreq"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 4 dereferenceable(68) %1, i8* noundef nonnull align 4 dereferenceable(68) %2, i64 68, i1 false)
  ret void
}

; <libc::unix::linux_like::arpreq_old as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN73_$LT$libc..unix..linux_like..arpreq_old$u20$as$u20$core..clone..Clone$GT$5clone17h44b547dee05babbfE"(%"unix::linux_like::arpreq_old"* noalias nocapture sret(%"unix::linux_like::arpreq_old") dereferenceable(52) %0, %"unix::linux_like::arpreq_old"* noalias nocapture readonly align 4 dereferenceable(52) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::arpreq_old"* %0 to i8*
  %2 = bitcast %"unix::linux_like::arpreq_old"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 4 dereferenceable(52) %1, i8* noundef nonnull align 4 dereferenceable(52) %2, i64 52, i1 false)
  ret void
}

; <libc::unix::linux_like::mmsghdr as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN70_$LT$libc..unix..linux_like..mmsghdr$u20$as$u20$core..clone..Clone$GT$5clone17h4d50e8030bfecddfE"(%"unix::linux_like::mmsghdr"* noalias nocapture sret(%"unix::linux_like::mmsghdr") dereferenceable(64) %0, %"unix::linux_like::mmsghdr"* noalias nocapture readonly align 8 dereferenceable(64) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::mmsghdr"* %0 to i8*
  %2 = bitcast %"unix::linux_like::mmsghdr"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(64) %1, i8* noundef nonnull align 8 dereferenceable(64) %2, i64 64, i1 false)
  ret void
}

; <libc::unix::linux_like::epoll_event as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define i96 @"_ZN74_$LT$libc..unix..linux_like..epoll_event$u20$as$u20$core..clone..Clone$GT$5clone17h33eae6f1f25c2d77E"(%"unix::linux_like::epoll_event"* noalias nocapture readonly align 1 dereferenceable(12) %self) unnamed_addr #2 {
start:
  %.sroa.0.0..sroa_cast = bitcast %"unix::linux_like::epoll_event"* %self to i96*
  %.sroa.0.0.copyload = load i96, i96* %.sroa.0.0..sroa_cast, align 1
  ret i96 %.sroa.0.0.copyload
}

; <libc::unix::linux_like::sockaddr_un as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN74_$LT$libc..unix..linux_like..sockaddr_un$u20$as$u20$core..clone..Clone$GT$5clone17he38c0ae28a97bf51E"(%"unix::linux_like::sockaddr_un"* noalias nocapture sret(%"unix::linux_like::sockaddr_un") dereferenceable(110) %0, %"unix::linux_like::sockaddr_un"* noalias nocapture readonly align 2 dereferenceable(110) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::sockaddr_un"* %0 to i8*
  %2 = bitcast %"unix::linux_like::sockaddr_un"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 2 dereferenceable(110) %1, i8* noundef nonnull align 2 dereferenceable(110) %2, i64 110, i1 false)
  ret void
}

; <libc::unix::linux_like::sockaddr_storage as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN79_$LT$libc..unix..linux_like..sockaddr_storage$u20$as$u20$core..clone..Clone$GT$5clone17h014bb721d5f511cfE"(%"unix::linux_like::sockaddr_storage"* noalias nocapture sret(%"unix::linux_like::sockaddr_storage") dereferenceable(128) %0, %"unix::linux_like::sockaddr_storage"* noalias nocapture readonly align 8 dereferenceable(128) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::sockaddr_storage"* %0 to i8*
  %2 = bitcast %"unix::linux_like::sockaddr_storage"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(128) %1, i8* noundef nonnull align 8 dereferenceable(128) %2, i64 128, i1 false)
  ret void
}

; <libc::unix::linux_like::utsname as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN70_$LT$libc..unix..linux_like..utsname$u20$as$u20$core..clone..Clone$GT$5clone17hee1b199d732e5730E"(%"unix::linux_like::utsname"* noalias nocapture sret(%"unix::linux_like::utsname") dereferenceable(390) %0, %"unix::linux_like::utsname"* noalias nocapture readonly align 1 dereferenceable(390) %self) unnamed_addr #1 {
start:
  %1 = getelementptr inbounds %"unix::linux_like::utsname", %"unix::linux_like::utsname"* %0, i64 0, i32 0, i64 0
  %2 = getelementptr inbounds %"unix::linux_like::utsname", %"unix::linux_like::utsname"* %self, i64 0, i32 0, i64 0
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 1 dereferenceable(390) %1, i8* noundef nonnull align 1 dereferenceable(390) %2, i64 390, i1 false)
  ret void
}

; <libc::unix::linux_like::sigevent as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN71_$LT$libc..unix..linux_like..sigevent$u20$as$u20$core..clone..Clone$GT$5clone17h27e4bbd6274690a2E"(%"unix::linux_like::sigevent"* noalias nocapture sret(%"unix::linux_like::sigevent") dereferenceable(64) %0, %"unix::linux_like::sigevent"* noalias nocapture readonly align 8 dereferenceable(64) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::sigevent"* %0 to i8*
  %2 = bitcast %"unix::linux_like::sigevent"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(64) %1, i8* noundef nonnull align 8 dereferenceable(64) %2, i64 64, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::glob_t as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN76_$LT$libc..unix..linux_like..linux..glob_t$u20$as$u20$core..clone..Clone$GT$5clone17h8e1ae773a5d987c2E"(%"unix::linux_like::linux::glob_t"* noalias nocapture sret(%"unix::linux_like::linux::glob_t") dereferenceable(72) %0, %"unix::linux_like::linux::glob_t"* noalias nocapture readonly align 8 dereferenceable(72) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::glob_t"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::glob_t"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(72) %1, i8* noundef nonnull align 8 dereferenceable(72) %2, i64 72, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::passwd as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN76_$LT$libc..unix..linux_like..linux..passwd$u20$as$u20$core..clone..Clone$GT$5clone17h37fc83de69a55a3aE"(%"unix::linux_like::linux::passwd"* noalias nocapture sret(%"unix::linux_like::linux::passwd") dereferenceable(48) %0, %"unix::linux_like::linux::passwd"* noalias nocapture readonly align 8 dereferenceable(48) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::passwd"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::passwd"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(48) %1, i8* noundef nonnull align 8 dereferenceable(48) %2, i64 48, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::spwd as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN74_$LT$libc..unix..linux_like..linux..spwd$u20$as$u20$core..clone..Clone$GT$5clone17h2ac5b5757326670dE"(%"unix::linux_like::linux::spwd"* noalias nocapture sret(%"unix::linux_like::linux::spwd") dereferenceable(72) %0, %"unix::linux_like::linux::spwd"* noalias nocapture readonly align 8 dereferenceable(72) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::spwd"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::spwd"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(72) %1, i8* noundef nonnull align 8 dereferenceable(72) %2, i64 72, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::dqblk as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN75_$LT$libc..unix..linux_like..linux..dqblk$u20$as$u20$core..clone..Clone$GT$5clone17h64edf94003cb6189E"(%"unix::linux_like::linux::dqblk"* noalias nocapture sret(%"unix::linux_like::linux::dqblk") dereferenceable(72) %0, %"unix::linux_like::linux::dqblk"* noalias nocapture readonly align 8 dereferenceable(72) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::dqblk"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::dqblk"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(72) %1, i8* noundef nonnull align 8 dereferenceable(72) %2, i64 72, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::signalfd_siginfo as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN86_$LT$libc..unix..linux_like..linux..signalfd_siginfo$u20$as$u20$core..clone..Clone$GT$5clone17h28e36599fe71738eE"(%"unix::linux_like::linux::signalfd_siginfo"* noalias nocapture sret(%"unix::linux_like::linux::signalfd_siginfo") dereferenceable(128) %0, %"unix::linux_like::linux::signalfd_siginfo"* noalias nocapture readonly align 8 dereferenceable(128) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::signalfd_siginfo"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::signalfd_siginfo"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(128) %1, i8* noundef nonnull align 8 dereferenceable(128) %2, i64 128, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::if_nameindex as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define { i32, i8* } @"_ZN82_$LT$libc..unix..linux_like..linux..if_nameindex$u20$as$u20$core..clone..Clone$GT$5clone17h00e3203ecf80090cE"({ i32, i8* }* noalias nocapture readonly align 8 dereferenceable(16) %self) unnamed_addr #2 {
start:
  %0 = getelementptr inbounds { i32, i8* }, { i32, i8* }* %self, i64 0, i32 0
  %1 = load i32, i32* %0, align 8
  %2 = getelementptr inbounds { i32, i8* }, { i32, i8* }* %self, i64 0, i32 1
  %3 = load i8*, i8** %2, align 8
  %4 = insertvalue { i32, i8* } undef, i32 %1, 0
  %5 = insertvalue { i32, i8* } %4, i8* %3, 1
  ret { i32, i8* } %5
}

; <libc::unix::linux_like::linux::msginfo as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN77_$LT$libc..unix..linux_like..linux..msginfo$u20$as$u20$core..clone..Clone$GT$5clone17h35349cc2bb7089f7E"(%"unix::linux_like::linux::msginfo"* noalias nocapture sret(%"unix::linux_like::linux::msginfo") dereferenceable(32) %0, %"unix::linux_like::linux::msginfo"* noalias nocapture readonly align 4 dereferenceable(32) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::msginfo"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::msginfo"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 4 dereferenceable(32) %1, i8* noundef nonnull align 4 dereferenceable(32) %2, i64 32, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::sembuf as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define i48 @"_ZN76_$LT$libc..unix..linux_like..linux..sembuf$u20$as$u20$core..clone..Clone$GT$5clone17h2138730f29c3fb15E"(%"unix::linux_like::linux::sembuf"* noalias nocapture readonly align 2 dereferenceable(6) %self) unnamed_addr #2 {
start:
  %.sroa.0.0..sroa_cast = bitcast %"unix::linux_like::linux::sembuf"* %self to i48*
  %.sroa.0.0.copyload = load i48, i48* %.sroa.0.0..sroa_cast, align 2
  ret i48 %.sroa.0.0.copyload
}

; <libc::unix::linux_like::linux::input_event as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN81_$LT$libc..unix..linux_like..linux..input_event$u20$as$u20$core..clone..Clone$GT$5clone17h712e345f6732cb58E"(%"unix::linux_like::linux::input_event"* noalias nocapture sret(%"unix::linux_like::linux::input_event") dereferenceable(24) %0, %"unix::linux_like::linux::input_event"* noalias nocapture readonly align 8 dereferenceable(24) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::input_event"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::input_event"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(24) %1, i8* noundef nonnull align 8 dereferenceable(24) %2, i64 24, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::input_absinfo as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN83_$LT$libc..unix..linux_like..linux..input_absinfo$u20$as$u20$core..clone..Clone$GT$5clone17h13a698fd16e6a92dE"(%"unix::linux_like::linux::input_absinfo"* noalias nocapture sret(%"unix::linux_like::linux::input_absinfo") dereferenceable(24) %0, %"unix::linux_like::linux::input_absinfo"* noalias nocapture readonly align 4 dereferenceable(24) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::input_absinfo"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::input_absinfo"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 4 dereferenceable(24) %1, i8* noundef nonnull align 4 dereferenceable(24) %2, i64 24, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::input_keymap_entry as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN88_$LT$libc..unix..linux_like..linux..input_keymap_entry$u20$as$u20$core..clone..Clone$GT$5clone17h70d296c7dea3c080E"(%"unix::linux_like::linux::input_keymap_entry"* noalias nocapture sret(%"unix::linux_like::linux::input_keymap_entry") dereferenceable(40) %0, %"unix::linux_like::linux::input_keymap_entry"* noalias nocapture readonly align 4 dereferenceable(40) %self) unnamed_addr #1 {
start:
  %1 = getelementptr inbounds %"unix::linux_like::linux::input_keymap_entry", %"unix::linux_like::linux::input_keymap_entry"* %0, i64 0, i32 0
  %2 = getelementptr inbounds %"unix::linux_like::linux::input_keymap_entry", %"unix::linux_like::linux::input_keymap_entry"* %self, i64 0, i32 0
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 4 dereferenceable(40) %1, i8* noundef nonnull align 4 dereferenceable(40) %2, i64 40, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::ff_constant_effect as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define i80 @"_ZN88_$LT$libc..unix..linux_like..linux..ff_constant_effect$u20$as$u20$core..clone..Clone$GT$5clone17h05a63fe04e70a302E"(%"unix::linux_like::linux::ff_constant_effect"* noalias nocapture readonly align 2 dereferenceable(10) %self) unnamed_addr #2 {
start:
  %.sroa.0.0..sroa_cast = bitcast %"unix::linux_like::linux::ff_constant_effect"* %self to i80*
  %.sroa.0.0.copyload = load i80, i80* %.sroa.0.0..sroa_cast, align 2
  ret i80 %.sroa.0.0.copyload
}

; <libc::unix::linux_like::linux::ff_ramp_effect as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define i96 @"_ZN84_$LT$libc..unix..linux_like..linux..ff_ramp_effect$u20$as$u20$core..clone..Clone$GT$5clone17heea59da34843e8daE"(%"unix::linux_like::linux::ff_ramp_effect"* noalias nocapture readonly align 2 dereferenceable(12) %self) unnamed_addr #2 {
start:
  %.sroa.0.0..sroa_cast = bitcast %"unix::linux_like::linux::ff_ramp_effect"* %self to i96*
  %.sroa.0.0.copyload = load i96, i96* %.sroa.0.0..sroa_cast, align 2
  ret i96 %.sroa.0.0.copyload
}

; <libc::unix::linux_like::linux::ff_periodic_effect as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN88_$LT$libc..unix..linux_like..linux..ff_periodic_effect$u20$as$u20$core..clone..Clone$GT$5clone17h83516dffe2172733E"(%"unix::linux_like::linux::ff_periodic_effect"* noalias nocapture sret(%"unix::linux_like::linux::ff_periodic_effect") dereferenceable(32) %0, %"unix::linux_like::linux::ff_periodic_effect"* noalias nocapture readonly align 8 dereferenceable(32) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::ff_periodic_effect"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::ff_periodic_effect"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(32) %1, i8* noundef nonnull align 8 dereferenceable(32) %2, i64 32, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::ff_effect as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN79_$LT$libc..unix..linux_like..linux..ff_effect$u20$as$u20$core..clone..Clone$GT$5clone17h588bae16cfc2d3c7E"(%"unix::linux_like::linux::ff_effect"* noalias nocapture sret(%"unix::linux_like::linux::ff_effect") dereferenceable(48) %0, %"unix::linux_like::linux::ff_effect"* noalias nocapture readonly align 8 dereferenceable(48) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::ff_effect"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::ff_effect"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(48) %1, i8* noundef nonnull align 8 dereferenceable(48) %2, i64 48, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::uinput_ff_upload as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN86_$LT$libc..unix..linux_like..linux..uinput_ff_upload$u20$as$u20$core..clone..Clone$GT$5clone17h408c901c74fb39eaE"(%"unix::linux_like::linux::uinput_ff_upload"* noalias nocapture sret(%"unix::linux_like::linux::uinput_ff_upload") dereferenceable(104) %0, %"unix::linux_like::linux::uinput_ff_upload"* noalias nocapture readonly align 8 dereferenceable(104) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::uinput_ff_upload"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::uinput_ff_upload"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(104) %1, i8* noundef nonnull align 8 dereferenceable(104) %2, i64 104, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::uinput_abs_setup as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN86_$LT$libc..unix..linux_like..linux..uinput_abs_setup$u20$as$u20$core..clone..Clone$GT$5clone17h8be54355984fe1b7E"(%"unix::linux_like::linux::uinput_abs_setup"* noalias nocapture sret(%"unix::linux_like::linux::uinput_abs_setup") dereferenceable(28) %0, %"unix::linux_like::linux::uinput_abs_setup"* noalias nocapture readonly align 4 dereferenceable(28) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::uinput_abs_setup"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::uinput_abs_setup"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 4 dereferenceable(28) %1, i8* noundef nonnull align 4 dereferenceable(28) %2, i64 28, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::dl_phdr_info as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN82_$LT$libc..unix..linux_like..linux..dl_phdr_info$u20$as$u20$core..clone..Clone$GT$5clone17h5f16f8e99e0ed905E"(%"unix::linux_like::linux::dl_phdr_info"* noalias nocapture sret(%"unix::linux_like::linux::dl_phdr_info") dereferenceable(64) %0, %"unix::linux_like::linux::dl_phdr_info"* noalias nocapture readonly align 8 dereferenceable(64) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::dl_phdr_info"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::dl_phdr_info"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(64) %1, i8* noundef nonnull align 8 dereferenceable(64) %2, i64 64, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::Elf32_Ehdr as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN80_$LT$libc..unix..linux_like..linux..Elf32_Ehdr$u20$as$u20$core..clone..Clone$GT$5clone17ha471c8f78d4af9b3E"(%"unix::linux_like::linux::Elf32_Ehdr"* noalias nocapture sret(%"unix::linux_like::linux::Elf32_Ehdr") dereferenceable(52) %0, %"unix::linux_like::linux::Elf32_Ehdr"* noalias nocapture readonly align 4 dereferenceable(52) %self) unnamed_addr #1 {
start:
  %1 = getelementptr inbounds %"unix::linux_like::linux::Elf32_Ehdr", %"unix::linux_like::linux::Elf32_Ehdr"* %0, i64 0, i32 0, i64 0
  %2 = getelementptr inbounds %"unix::linux_like::linux::Elf32_Ehdr", %"unix::linux_like::linux::Elf32_Ehdr"* %self, i64 0, i32 0, i64 0
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 4 dereferenceable(52) %1, i8* noundef nonnull align 4 dereferenceable(52) %2, i64 52, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::Elf64_Ehdr as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN80_$LT$libc..unix..linux_like..linux..Elf64_Ehdr$u20$as$u20$core..clone..Clone$GT$5clone17h7bd2bbde7605cdfeE"(%"unix::linux_like::linux::Elf64_Ehdr"* noalias nocapture sret(%"unix::linux_like::linux::Elf64_Ehdr") dereferenceable(64) %0, %"unix::linux_like::linux::Elf64_Ehdr"* noalias nocapture readonly align 8 dereferenceable(64) %self) unnamed_addr #1 {
start:
  %1 = getelementptr inbounds %"unix::linux_like::linux::Elf64_Ehdr", %"unix::linux_like::linux::Elf64_Ehdr"* %0, i64 0, i32 0, i64 0
  %2 = getelementptr inbounds %"unix::linux_like::linux::Elf64_Ehdr", %"unix::linux_like::linux::Elf64_Ehdr"* %self, i64 0, i32 0, i64 0
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(64) %1, i8* noundef nonnull align 8 dereferenceable(64) %2, i64 64, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::Elf64_Sym as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN79_$LT$libc..unix..linux_like..linux..Elf64_Sym$u20$as$u20$core..clone..Clone$GT$5clone17hf82f8b46f31c6bdbE"(%"unix::linux_like::linux::Elf64_Sym"* noalias nocapture sret(%"unix::linux_like::linux::Elf64_Sym") dereferenceable(24) %0, %"unix::linux_like::linux::Elf64_Sym"* noalias nocapture readonly align 8 dereferenceable(24) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::Elf64_Sym"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::Elf64_Sym"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(24) %1, i8* noundef nonnull align 8 dereferenceable(24) %2, i64 24, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::Elf32_Phdr as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN80_$LT$libc..unix..linux_like..linux..Elf32_Phdr$u20$as$u20$core..clone..Clone$GT$5clone17h16a97a4b256a176dE"(%"unix::linux_like::linux::Elf32_Phdr"* noalias nocapture sret(%"unix::linux_like::linux::Elf32_Phdr") dereferenceable(32) %0, %"unix::linux_like::linux::Elf32_Phdr"* noalias nocapture readonly align 4 dereferenceable(32) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::Elf32_Phdr"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::Elf32_Phdr"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 4 dereferenceable(32) %1, i8* noundef nonnull align 4 dereferenceable(32) %2, i64 32, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::Elf64_Phdr as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN80_$LT$libc..unix..linux_like..linux..Elf64_Phdr$u20$as$u20$core..clone..Clone$GT$5clone17hce38db51235775d4E"(%"unix::linux_like::linux::Elf64_Phdr"* noalias nocapture sret(%"unix::linux_like::linux::Elf64_Phdr") dereferenceable(56) %0, %"unix::linux_like::linux::Elf64_Phdr"* noalias nocapture readonly align 8 dereferenceable(56) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::Elf64_Phdr"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::Elf64_Phdr"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(56) %1, i8* noundef nonnull align 8 dereferenceable(56) %2, i64 56, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::Elf32_Shdr as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN80_$LT$libc..unix..linux_like..linux..Elf32_Shdr$u20$as$u20$core..clone..Clone$GT$5clone17hb0030e1715d5404eE"(%"unix::linux_like::linux::Elf32_Shdr"* noalias nocapture sret(%"unix::linux_like::linux::Elf32_Shdr") dereferenceable(40) %0, %"unix::linux_like::linux::Elf32_Shdr"* noalias nocapture readonly align 4 dereferenceable(40) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::Elf32_Shdr"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::Elf32_Shdr"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 4 dereferenceable(40) %1, i8* noundef nonnull align 4 dereferenceable(40) %2, i64 40, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::Elf64_Shdr as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN80_$LT$libc..unix..linux_like..linux..Elf64_Shdr$u20$as$u20$core..clone..Clone$GT$5clone17h2af6acf4358e867cE"(%"unix::linux_like::linux::Elf64_Shdr"* noalias nocapture sret(%"unix::linux_like::linux::Elf64_Shdr") dereferenceable(64) %0, %"unix::linux_like::linux::Elf64_Shdr"* noalias nocapture readonly align 8 dereferenceable(64) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::Elf64_Shdr"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::Elf64_Shdr"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(64) %1, i8* noundef nonnull align 8 dereferenceable(64) %2, i64 64, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::mntent as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN76_$LT$libc..unix..linux_like..linux..mntent$u20$as$u20$core..clone..Clone$GT$5clone17h726bbf36054df0e6E"(%"unix::linux_like::linux::mntent"* noalias nocapture sret(%"unix::linux_like::linux::mntent") dereferenceable(40) %0, %"unix::linux_like::linux::mntent"* noalias nocapture readonly align 8 dereferenceable(40) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::mntent"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::mntent"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(40) %1, i8* noundef nonnull align 8 dereferenceable(40) %2, i64 40, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::posix_spawn_file_actions_t as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN96_$LT$libc..unix..linux_like..linux..posix_spawn_file_actions_t$u20$as$u20$core..clone..Clone$GT$5clone17h5fd1cb24a25fe512E"(%"unix::linux_like::linux::posix_spawn_file_actions_t"* noalias nocapture sret(%"unix::linux_like::linux::posix_spawn_file_actions_t") dereferenceable(80) %0, %"unix::linux_like::linux::posix_spawn_file_actions_t"* noalias nocapture readonly align 8 dereferenceable(80) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::posix_spawn_file_actions_t"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::posix_spawn_file_actions_t"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(80) %1, i8* noundef nonnull align 8 dereferenceable(80) %2, i64 80, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::posix_spawnattr_t as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN87_$LT$libc..unix..linux_like..linux..posix_spawnattr_t$u20$as$u20$core..clone..Clone$GT$5clone17hcac42c40550ad551E"(%"unix::linux_like::linux::posix_spawnattr_t"* noalias nocapture sret(%"unix::linux_like::linux::posix_spawnattr_t") dereferenceable(336) %0, %"unix::linux_like::linux::posix_spawnattr_t"* noalias nocapture readonly align 8 dereferenceable(336) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::posix_spawnattr_t"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::posix_spawnattr_t"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(336) %1, i8* noundef nonnull align 8 dereferenceable(336) %2, i64 336, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::genlmsghdr as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define i32 @"_ZN80_$LT$libc..unix..linux_like..linux..genlmsghdr$u20$as$u20$core..clone..Clone$GT$5clone17hae7154d4d820b6d7E"(%"unix::linux_like::linux::genlmsghdr"* noalias nocapture readonly align 2 dereferenceable(4) %self) unnamed_addr #2 {
start:
  %.sroa.0.0..sroa_cast = bitcast %"unix::linux_like::linux::genlmsghdr"* %self to i32*
  %.sroa.0.0.copyload = load i32, i32* %.sroa.0.0..sroa_cast, align 2
  ret i32 %.sroa.0.0.copyload
}

; <libc::unix::linux_like::linux::arpd_request as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN82_$LT$libc..unix..linux_like..linux..arpd_request$u20$as$u20$core..clone..Clone$GT$5clone17hbf1e4cb7eafad3b6E"(%"unix::linux_like::linux::arpd_request"* noalias nocapture sret(%"unix::linux_like::linux::arpd_request") dereferenceable(40) %0, %"unix::linux_like::linux::arpd_request"* noalias nocapture readonly align 8 dereferenceable(40) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::arpd_request"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::arpd_request"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(40) %1, i8* noundef nonnull align 8 dereferenceable(40) %2, i64 40, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::__c_anonymous_sockaddr_can_j1939 as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define i128 @"_ZN102_$LT$libc..unix..linux_like..linux..__c_anonymous_sockaddr_can_j1939$u20$as$u20$core..clone..Clone$GT$5clone17h141014a64733d813E"(%"unix::linux_like::linux::__c_anonymous_sockaddr_can_j1939"* noalias nocapture readonly align 8 dereferenceable(16) %self) unnamed_addr #2 {
start:
  %.sroa.0.0..sroa_cast = bitcast %"unix::linux_like::linux::__c_anonymous_sockaddr_can_j1939"* %self to i128*
  %.sroa.0.0.copyload = load i128, i128* %.sroa.0.0..sroa_cast, align 8
  ret i128 %.sroa.0.0.copyload
}

; <libc::unix::linux_like::linux::sock_fprog as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define { i16, i32* } @"_ZN80_$LT$libc..unix..linux_like..linux..sock_fprog$u20$as$u20$core..clone..Clone$GT$5clone17hd37be03d84b7be9cE"({ i16, i32* }* noalias nocapture readonly align 8 dereferenceable(16) %self) unnamed_addr #2 {
start:
  %0 = getelementptr inbounds { i16, i32* }, { i16, i32* }* %self, i64 0, i32 0
  %1 = load i16, i16* %0, align 8
  %2 = getelementptr inbounds { i16, i32* }, { i16, i32* }* %self, i64 0, i32 1
  %3 = load i32*, i32** %2, align 8
  %4 = insertvalue { i16, i32* } undef, i16 %1, 0
  %5 = insertvalue { i16, i32* } %4, i32* %3, 1
  ret { i16, i32* } %5
}

; <libc::unix::linux_like::linux::seccomp_data as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN82_$LT$libc..unix..linux_like..linux..seccomp_data$u20$as$u20$core..clone..Clone$GT$5clone17h619a24491c7027b5E"(%"unix::linux_like::linux::seccomp_data"* noalias nocapture sret(%"unix::linux_like::linux::seccomp_data") dereferenceable(64) %0, %"unix::linux_like::linux::seccomp_data"* noalias nocapture readonly align 8 dereferenceable(64) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::seccomp_data"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::seccomp_data"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(64) %1, i8* noundef nonnull align 8 dereferenceable(64) %2, i64 64, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::nlmsgerr as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN78_$LT$libc..unix..linux_like..linux..nlmsgerr$u20$as$u20$core..clone..Clone$GT$5clone17h1a410bbcd7d66167E"(%"unix::linux_like::linux::nlmsgerr"* noalias nocapture sret(%"unix::linux_like::linux::nlmsgerr") dereferenceable(20) %0, %"unix::linux_like::linux::nlmsgerr"* noalias nocapture readonly align 4 dereferenceable(20) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::nlmsgerr"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::nlmsgerr"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 4 dereferenceable(20) %1, i8* noundef nonnull align 4 dereferenceable(20) %2, i64 20, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::nlattr as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define { i16, i16 } @"_ZN76_$LT$libc..unix..linux_like..linux..nlattr$u20$as$u20$core..clone..Clone$GT$5clone17hd4f52d91c333441fE"({ i16, i16 }* noalias nocapture readonly align 2 dereferenceable(4) %self) unnamed_addr #2 {
start:
  %0 = getelementptr inbounds { i16, i16 }, { i16, i16 }* %self, i64 0, i32 0
  %1 = load i16, i16* %0, align 2
  %2 = getelementptr inbounds { i16, i16 }, { i16, i16 }* %self, i64 0, i32 1
  %3 = load i16, i16* %2, align 2
  %4 = insertvalue { i16, i16 } undef, i16 %1, 0
  %5 = insertvalue { i16, i16 } %4, i16 %3, 1
  ret { i16, i16 } %5
}

; <libc::unix::linux_like::linux::dirent as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN76_$LT$libc..unix..linux_like..linux..dirent$u20$as$u20$core..clone..Clone$GT$5clone17h28b4931108e6f334E"(%"unix::linux_like::linux::dirent"* noalias nocapture sret(%"unix::linux_like::linux::dirent") dereferenceable(280) %0, %"unix::linux_like::linux::dirent"* noalias nocapture readonly align 8 dereferenceable(280) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::dirent"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::dirent"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(280) %1, i8* noundef nonnull align 8 dereferenceable(280) %2, i64 280, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::sockaddr_alg as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN82_$LT$libc..unix..linux_like..linux..sockaddr_alg$u20$as$u20$core..clone..Clone$GT$5clone17h214726e22325bf49E"(%"unix::linux_like::linux::sockaddr_alg"* noalias nocapture sret(%"unix::linux_like::linux::sockaddr_alg") dereferenceable(88) %0, %"unix::linux_like::linux::sockaddr_alg"* noalias nocapture readonly align 4 dereferenceable(88) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::sockaddr_alg"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::sockaddr_alg"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 4 dereferenceable(88) %1, i8* noundef nonnull align 4 dereferenceable(88) %2, i64 88, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::uinput_setup as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN82_$LT$libc..unix..linux_like..linux..uinput_setup$u20$as$u20$core..clone..Clone$GT$5clone17h1ac6e7867068067aE"(%"unix::linux_like::linux::uinput_setup"* noalias nocapture sret(%"unix::linux_like::linux::uinput_setup") dereferenceable(92) %0, %"unix::linux_like::linux::uinput_setup"* noalias nocapture readonly align 4 dereferenceable(92) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::uinput_setup"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::uinput_setup"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 4 dereferenceable(92) %1, i8* noundef nonnull align 4 dereferenceable(92) %2, i64 92, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::uinput_user_dev as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN85_$LT$libc..unix..linux_like..linux..uinput_user_dev$u20$as$u20$core..clone..Clone$GT$5clone17ha803cfa7b4231200E"(%"unix::linux_like::linux::uinput_user_dev"* noalias nocapture sret(%"unix::linux_like::linux::uinput_user_dev") dereferenceable(1116) %0, %"unix::linux_like::linux::uinput_user_dev"* noalias nocapture readonly align 4 dereferenceable(1116) %self) unnamed_addr #1 {
start:
  %1 = getelementptr inbounds %"unix::linux_like::linux::uinput_user_dev", %"unix::linux_like::linux::uinput_user_dev"* %0, i64 0, i32 0, i64 0
  %2 = getelementptr inbounds %"unix::linux_like::linux::uinput_user_dev", %"unix::linux_like::linux::uinput_user_dev"* %self, i64 0, i32 0, i64 0
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 4 dereferenceable(1116) %1, i8* noundef nonnull align 4 dereferenceable(1116) %2, i64 1116, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::mq_attr as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN77_$LT$libc..unix..linux_like..linux..mq_attr$u20$as$u20$core..clone..Clone$GT$5clone17h8eaad9a925c8d451E"(%"unix::linux_like::linux::mq_attr"* noalias nocapture sret(%"unix::linux_like::linux::mq_attr") dereferenceable(64) %0, %"unix::linux_like::linux::mq_attr"* noalias nocapture readonly align 8 dereferenceable(64) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::mq_attr"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::mq_attr"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(64) %1, i8* noundef nonnull align 8 dereferenceable(64) %2, i64 64, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::sockaddr_can as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN82_$LT$libc..unix..linux_like..linux..sockaddr_can$u20$as$u20$core..clone..Clone$GT$5clone17hb6b8bbd9616ee757E"(%"unix::linux_like::linux::sockaddr_can"* noalias nocapture sret(%"unix::linux_like::linux::sockaddr_can") dereferenceable(24) %0, %"unix::linux_like::linux::sockaddr_can"* noalias nocapture readonly align 8 dereferenceable(24) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::sockaddr_can"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::sockaddr_can"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(24) %1, i8* noundef nonnull align 8 dereferenceable(24) %2, i64 24, i1 false)
  ret void
}

; libc::unix::linux_like::linux::gnu::<impl libc::unix::linux_like::linux::gnu::b64::x86_64::siginfo_t>::si_addr
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define i8* @"_ZN4libc4unix10linux_like5linux3gnu76_$LT$impl$u20$libc..unix..linux_like..linux..gnu..b64..x86_64..siginfo_t$GT$7si_addr17h40adca618da0fa0dE"(%"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t"* noalias nocapture readonly align 8 dereferenceable(128) %self) unnamed_addr #2 {
start:
  %0 = getelementptr inbounds %"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t", %"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t"* %self, i64 0, i32 3, i64 1
  %1 = bitcast i32* %0 to i8**
  %2 = load i8*, i8** %1, align 8
  ret i8* %2
}

; libc::unix::linux_like::linux::gnu::<impl libc::unix::linux_like::linux::gnu::b64::x86_64::siginfo_t>::si_value
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define i64 @"_ZN4libc4unix10linux_like5linux3gnu76_$LT$impl$u20$libc..unix..linux_like..linux..gnu..b64..x86_64..siginfo_t$GT$8si_value17h5b7e03c8bc91adbdE"(%"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t"* noalias nocapture readonly align 8 dereferenceable(128) %self) unnamed_addr #2 {
start:
  %.sroa.0.0..sroa_idx1 = getelementptr inbounds %"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t", %"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t"* %self, i64 0, i32 3, i64 3
  %.sroa.0.0..sroa_cast = bitcast i32* %.sroa.0.0..sroa_idx1 to i64*
  %.sroa.0.0.copyload = load i64, i64* %.sroa.0.0..sroa_cast, align 8
  ret i64 %.sroa.0.0.copyload
}

; <libc::unix::linux_like::linux::gnu::statx as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN80_$LT$libc..unix..linux_like..linux..gnu..statx$u20$as$u20$core..clone..Clone$GT$5clone17h640aee338f14c6c3E"(%"unix::linux_like::linux::gnu::statx"* noalias nocapture sret(%"unix::linux_like::linux::gnu::statx") dereferenceable(256) %0, %"unix::linux_like::linux::gnu::statx"* noalias nocapture readonly align 8 dereferenceable(256) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::statx"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::statx"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(256) %1, i8* noundef nonnull align 8 dereferenceable(256) %2, i64 256, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::aiocb as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN80_$LT$libc..unix..linux_like..linux..gnu..aiocb$u20$as$u20$core..clone..Clone$GT$5clone17h9cc157abb64e0df9E"(%"unix::linux_like::linux::gnu::aiocb"* noalias nocapture sret(%"unix::linux_like::linux::gnu::aiocb") dereferenceable(168) %0, %"unix::linux_like::linux::gnu::aiocb"* noalias nocapture readonly align 8 dereferenceable(168) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::aiocb"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::aiocb"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(168) %1, i8* noundef nonnull align 8 dereferenceable(168) %2, i64 168, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::msghdr as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN81_$LT$libc..unix..linux_like..linux..gnu..msghdr$u20$as$u20$core..clone..Clone$GT$5clone17h1d22f4c947902797E"(%"unix::linux_like::linux::gnu::msghdr"* noalias nocapture sret(%"unix::linux_like::linux::gnu::msghdr") dereferenceable(56) %0, %"unix::linux_like::linux::gnu::msghdr"* noalias nocapture readonly align 8 dereferenceable(56) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::msghdr"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::msghdr"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(56) %1, i8* noundef nonnull align 8 dereferenceable(56) %2, i64 56, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::termios as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN82_$LT$libc..unix..linux_like..linux..gnu..termios$u20$as$u20$core..clone..Clone$GT$5clone17hda33df43594e400eE"(%"unix::linux_like::linux::gnu::termios"* noalias nocapture sret(%"unix::linux_like::linux::gnu::termios") dereferenceable(60) %0, %"unix::linux_like::linux::gnu::termios"* noalias nocapture readonly align 4 dereferenceable(60) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::termios"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::termios"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 4 dereferenceable(60) %1, i8* noundef nonnull align 4 dereferenceable(60) %2, i64 60, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::mallinfo2 as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN84_$LT$libc..unix..linux_like..linux..gnu..mallinfo2$u20$as$u20$core..clone..Clone$GT$5clone17hed8daed460572e04E"(%"unix::linux_like::linux::gnu::mallinfo2"* noalias nocapture sret(%"unix::linux_like::linux::gnu::mallinfo2") dereferenceable(80) %0, %"unix::linux_like::linux::gnu::mallinfo2"* noalias nocapture readonly align 8 dereferenceable(80) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::mallinfo2"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::mallinfo2"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(80) %1, i8* noundef nonnull align 8 dereferenceable(80) %2, i64 80, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::rtentry as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN82_$LT$libc..unix..linux_like..linux..gnu..rtentry$u20$as$u20$core..clone..Clone$GT$5clone17h859cba8cbabf7913E"(%"unix::linux_like::linux::gnu::rtentry"* noalias nocapture sret(%"unix::linux_like::linux::gnu::rtentry") dereferenceable(120) %0, %"unix::linux_like::linux::gnu::rtentry"* noalias nocapture readonly align 8 dereferenceable(120) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::rtentry"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::rtentry"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(120) %1, i8* noundef nonnull align 8 dereferenceable(120) %2, i64 120, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::timex as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN80_$LT$libc..unix..linux_like..linux..gnu..timex$u20$as$u20$core..clone..Clone$GT$5clone17h7df69a27e4387195E"(%"unix::linux_like::linux::gnu::timex"* noalias nocapture sret(%"unix::linux_like::linux::gnu::timex") dereferenceable(208) %0, %"unix::linux_like::linux::gnu::timex"* noalias nocapture readonly align 8 dereferenceable(208) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::timex"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::timex"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(208) %1, i8* noundef nonnull align 8 dereferenceable(208) %2, i64 208, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::ntptimeval as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN85_$LT$libc..unix..linux_like..linux..gnu..ntptimeval$u20$as$u20$core..clone..Clone$GT$5clone17hbefd380090d4dc5aE"(%"unix::linux_like::linux::gnu::ntptimeval"* noalias nocapture sret(%"unix::linux_like::linux::gnu::ntptimeval") dereferenceable(72) %0, %"unix::linux_like::linux::gnu::ntptimeval"* noalias nocapture readonly align 8 dereferenceable(72) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::ntptimeval"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::ntptimeval"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(72) %1, i8* noundef nonnull align 8 dereferenceable(72) %2, i64 72, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::regex_t as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN82_$LT$libc..unix..linux_like..linux..gnu..regex_t$u20$as$u20$core..clone..Clone$GT$5clone17h81e9c9434b682090E"(%"unix::linux_like::linux::gnu::regex_t"* noalias nocapture sret(%"unix::linux_like::linux::gnu::regex_t") dereferenceable(64) %0, %"unix::linux_like::linux::gnu::regex_t"* noalias nocapture readonly align 8 dereferenceable(64) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::regex_t"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::regex_t"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(64) %1, i8* noundef nonnull align 8 dereferenceable(64) %2, i64 64, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::Elf64_Chdr as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN85_$LT$libc..unix..linux_like..linux..gnu..Elf64_Chdr$u20$as$u20$core..clone..Clone$GT$5clone17h0fbb07554e0bb9f3E"(%"unix::linux_like::linux::gnu::Elf64_Chdr"* noalias nocapture sret(%"unix::linux_like::linux::gnu::Elf64_Chdr") dereferenceable(24) %0, %"unix::linux_like::linux::gnu::Elf64_Chdr"* noalias nocapture readonly align 8 dereferenceable(24) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::Elf64_Chdr"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::Elf64_Chdr"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(24) %1, i8* noundef nonnull align 8 dereferenceable(24) %2, i64 24, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::sifields_sigchld as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN91_$LT$libc..unix..linux_like..linux..gnu..sifields_sigchld$u20$as$u20$core..clone..Clone$GT$5clone17h5e725f485e4ec34dE"(%"unix::linux_like::linux::gnu::sifields_sigchld"* noalias nocapture sret(%"unix::linux_like::linux::gnu::sifields_sigchld") dereferenceable(32) %0, %"unix::linux_like::linux::gnu::sifields_sigchld"* noalias nocapture readonly align 8 dereferenceable(32) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::sifields_sigchld"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::sifields_sigchld"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(32) %1, i8* noundef nonnull align 8 dereferenceable(32) %2, i64 32, i1 false)
  ret void
}

; libc::unix::linux_like::linux::gnu::<impl libc::unix::linux_like::linux::gnu::b64::x86_64::siginfo_t>::si_pid
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define i32 @"_ZN4libc4unix10linux_like5linux3gnu76_$LT$impl$u20$libc..unix..linux_like..linux..gnu..b64..x86_64..siginfo_t$GT$6si_pid17h8841695e897b5410E"(%"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t"* noalias nocapture readonly align 8 dereferenceable(128) %self) unnamed_addr #1 {
start:
  %0 = getelementptr inbounds %"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t", %"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t"* %self, i64 0, i32 3, i64 1
  %1 = load i32, i32* %0, align 8
  ret i32 %1
}

; libc::unix::linux_like::linux::gnu::<impl libc::unix::linux_like::linux::gnu::b64::x86_64::siginfo_t>::si_uid
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define i32 @"_ZN4libc4unix10linux_like5linux3gnu76_$LT$impl$u20$libc..unix..linux_like..linux..gnu..b64..x86_64..siginfo_t$GT$6si_uid17h42bdf8dd0f6d4797E"(%"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t"* noalias nocapture readonly align 8 dereferenceable(128) %self) unnamed_addr #1 {
start:
  %0 = getelementptr inbounds %"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t", %"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t"* %self, i64 0, i32 3, i64 2
  %1 = load i32, i32* %0, align 4
  ret i32 %1
}

; libc::unix::linux_like::linux::gnu::<impl libc::unix::linux_like::linux::gnu::b64::x86_64::siginfo_t>::si_status
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define i32 @"_ZN4libc4unix10linux_like5linux3gnu76_$LT$impl$u20$libc..unix..linux_like..linux..gnu..b64..x86_64..siginfo_t$GT$9si_status17h1a181e63dbcc0462E"(%"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t"* noalias nocapture readonly align 8 dereferenceable(128) %self) unnamed_addr #1 {
start:
  %0 = getelementptr inbounds %"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t", %"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t"* %self, i64 0, i32 3, i64 3
  %1 = load i32, i32* %0, align 8
  ret i32 %1
}

; libc::unix::linux_like::linux::gnu::<impl libc::unix::linux_like::linux::gnu::b64::x86_64::siginfo_t>::si_utime
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define i64 @"_ZN4libc4unix10linux_like5linux3gnu76_$LT$impl$u20$libc..unix..linux_like..linux..gnu..b64..x86_64..siginfo_t$GT$8si_utime17h35e647c6c32624b0E"(%"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t"* noalias nocapture readonly align 8 dereferenceable(128) %self) unnamed_addr #1 {
start:
  %0 = getelementptr inbounds %"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t", %"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t"* %self, i64 0, i32 3, i64 5
  %1 = bitcast i32* %0 to i64*
  %2 = load i64, i64* %1, align 8
  ret i64 %2
}

; libc::unix::linux_like::linux::gnu::<impl libc::unix::linux_like::linux::gnu::b64::x86_64::siginfo_t>::si_stime
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define i64 @"_ZN4libc4unix10linux_like5linux3gnu76_$LT$impl$u20$libc..unix..linux_like..linux..gnu..b64..x86_64..siginfo_t$GT$8si_stime17h954351a19435eb29E"(%"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t"* noalias nocapture readonly align 8 dereferenceable(128) %self) unnamed_addr #1 {
start:
  %0 = getelementptr inbounds %"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t", %"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t"* %self, i64 0, i32 3, i64 7
  %1 = bitcast i32* %0 to i64*
  %2 = load i64, i64* %1, align 8
  ret i64 %2
}

; <libc::unix::linux_like::linux::gnu::utmpx as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN80_$LT$libc..unix..linux_like..linux..gnu..utmpx$u20$as$u20$core..clone..Clone$GT$5clone17hacf6b1b9c19ca610E"(%"unix::linux_like::linux::gnu::utmpx"* noalias nocapture sret(%"unix::linux_like::linux::gnu::utmpx") dereferenceable(384) %0, %"unix::linux_like::linux::gnu::utmpx"* noalias nocapture readonly align 4 dereferenceable(384) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::utmpx"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::utmpx"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 4 dereferenceable(384) %1, i8* noundef nonnull align 4 dereferenceable(384) %2, i64 384, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::sysinfo as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN87_$LT$libc..unix..linux_like..linux..gnu..b64..sysinfo$u20$as$u20$core..clone..Clone$GT$5clone17hb45c938496966fb0E"(%"unix::linux_like::linux::gnu::b64::sysinfo"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::sysinfo") dereferenceable(112) %0, %"unix::linux_like::linux::gnu::b64::sysinfo"* noalias nocapture readonly align 8 dereferenceable(112) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::sysinfo"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::sysinfo"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(112) %1, i8* noundef nonnull align 8 dereferenceable(112) %2, i64 112, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::msqid_ds as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN88_$LT$libc..unix..linux_like..linux..gnu..b64..msqid_ds$u20$as$u20$core..clone..Clone$GT$5clone17h00544eec466c37a2E"(%"unix::linux_like::linux::gnu::b64::msqid_ds"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::msqid_ds") dereferenceable(120) %0, %"unix::linux_like::linux::gnu::b64::msqid_ds"* noalias nocapture readonly align 8 dereferenceable(120) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::msqid_ds"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::msqid_ds"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(120) %1, i8* noundef nonnull align 8 dereferenceable(120) %2, i64 120, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::semid_ds as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN88_$LT$libc..unix..linux_like..linux..gnu..b64..semid_ds$u20$as$u20$core..clone..Clone$GT$5clone17h7e8d2c0ce5202adaE"(%"unix::linux_like::linux::gnu::b64::semid_ds"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::semid_ds") dereferenceable(104) %0, %"unix::linux_like::linux::gnu::b64::semid_ds"* noalias nocapture readonly align 8 dereferenceable(104) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::semid_ds"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::semid_ds"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(104) %1, i8* noundef nonnull align 8 dereferenceable(104) %2, i64 104, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::x86_64::sigaction as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN97_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..sigaction$u20$as$u20$core..clone..Clone$GT$5clone17hebfeb08900797f68E"(%"unix::linux_like::linux::gnu::b64::x86_64::sigaction"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::x86_64::sigaction") dereferenceable(152) %0, %"unix::linux_like::linux::gnu::b64::x86_64::sigaction"* noalias nocapture readonly align 8 dereferenceable(152) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::sigaction"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::sigaction"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(152) %1, i8* noundef nonnull align 8 dereferenceable(152) %2, i64 152, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::x86_64::statfs as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN94_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..statfs$u20$as$u20$core..clone..Clone$GT$5clone17h391f60ede1bb69a0E"(%"unix::linux_like::linux::gnu::b64::x86_64::statfs"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::x86_64::statfs") dereferenceable(120) %0, %"unix::linux_like::linux::gnu::b64::x86_64::statfs"* noalias nocapture readonly align 8 dereferenceable(120) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::statfs"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::statfs"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(120) %1, i8* noundef nonnull align 8 dereferenceable(120) %2, i64 120, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::x86_64::flock as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN93_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..flock$u20$as$u20$core..clone..Clone$GT$5clone17h3425a14370c6df63E"(%"unix::linux_like::linux::gnu::b64::x86_64::flock"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::x86_64::flock") dereferenceable(32) %0, %"unix::linux_like::linux::gnu::b64::x86_64::flock"* noalias nocapture readonly align 8 dereferenceable(32) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::flock"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::flock"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(32) %1, i8* noundef nonnull align 8 dereferenceable(32) %2, i64 32, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::x86_64::siginfo_t as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN97_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..siginfo_t$u20$as$u20$core..clone..Clone$GT$5clone17h3ef57b3f04841183E"(%"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t") dereferenceable(128) %0, %"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t"* noalias nocapture readonly align 8 dereferenceable(128) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::siginfo_t"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(128) %1, i8* noundef nonnull align 8 dereferenceable(128) %2, i64 128, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::x86_64::stack_t as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN95_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..stack_t$u20$as$u20$core..clone..Clone$GT$5clone17h404dcc8b2fcd2f7bE"(%"unix::linux_like::linux::gnu::b64::x86_64::stack_t"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::x86_64::stack_t") dereferenceable(24) %0, %"unix::linux_like::linux::gnu::b64::x86_64::stack_t"* noalias nocapture readonly align 8 dereferenceable(24) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::stack_t"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::stack_t"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(24) %1, i8* noundef nonnull align 8 dereferenceable(24) %2, i64 24, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::x86_64::stat as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN92_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..stat$u20$as$u20$core..clone..Clone$GT$5clone17h3bd98f2fcfa01778E"(%"unix::linux_like::linux::gnu::b64::x86_64::stat"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::x86_64::stat") dereferenceable(144) %0, %"unix::linux_like::linux::gnu::b64::x86_64::stat"* noalias nocapture readonly align 8 dereferenceable(144) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::stat"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::stat"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(144) %1, i8* noundef nonnull align 8 dereferenceable(144) %2, i64 144, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::x86_64::statfs64 as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN96_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..statfs64$u20$as$u20$core..clone..Clone$GT$5clone17h7122ccd34b32df05E"(%"unix::linux_like::linux::gnu::b64::x86_64::statfs64"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::x86_64::statfs64") dereferenceable(120) %0, %"unix::linux_like::linux::gnu::b64::x86_64::statfs64"* noalias nocapture readonly align 8 dereferenceable(120) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::statfs64"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::statfs64"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(120) %1, i8* noundef nonnull align 8 dereferenceable(120) %2, i64 120, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::x86_64::pthread_attr_t as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN102_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..pthread_attr_t$u20$as$u20$core..clone..Clone$GT$5clone17h64ee7d24d7c1fa10E"(%"unix::linux_like::linux::gnu::b64::x86_64::pthread_attr_t"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::x86_64::pthread_attr_t") dereferenceable(56) %0, %"unix::linux_like::linux::gnu::b64::x86_64::pthread_attr_t"* noalias nocapture readonly align 8 dereferenceable(56) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::pthread_attr_t"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::pthread_attr_t"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(56) %1, i8* noundef nonnull align 8 dereferenceable(56) %2, i64 56, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::x86_64::_libc_fpxreg as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define i128 @"_ZN100_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64.._libc_fpxreg$u20$as$u20$core..clone..Clone$GT$5clone17h0713ba9d1aff421bE"(%"unix::linux_like::linux::gnu::b64::x86_64::_libc_fpxreg"* noalias nocapture readonly align 2 dereferenceable(16) %self) unnamed_addr #2 {
start:
  %.sroa.0.0..sroa_cast = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::_libc_fpxreg"* %self to i128*
  %.sroa.0.0.copyload = load i128, i128* %.sroa.0.0..sroa_cast, align 2
  ret i128 %.sroa.0.0.copyload
}

; <libc::unix::linux_like::linux::gnu::b64::x86_64::_libc_xmmreg as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define i128 @"_ZN100_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64.._libc_xmmreg$u20$as$u20$core..clone..Clone$GT$5clone17h29a5cf18b565b38fE"(%"unix::linux_like::linux::gnu::b64::x86_64::_libc_xmmreg"* noalias nocapture readonly align 4 dereferenceable(16) %self) unnamed_addr #2 {
start:
  %.sroa.0.0..sroa_cast = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::_libc_xmmreg"* %self to i128*
  %.sroa.0.0.copyload = load i128, i128* %.sroa.0.0..sroa_cast, align 4
  ret i128 %.sroa.0.0.copyload
}

; <libc::unix::linux_like::linux::gnu::b64::x86_64::_libc_fpstate as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN101_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64.._libc_fpstate$u20$as$u20$core..clone..Clone$GT$5clone17hf040eb87cb4a4ff9E"(%"unix::linux_like::linux::gnu::b64::x86_64::_libc_fpstate"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::x86_64::_libc_fpstate") dereferenceable(512) %0, %"unix::linux_like::linux::gnu::b64::x86_64::_libc_fpstate"* noalias nocapture readonly align 8 dereferenceable(512) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::_libc_fpstate"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::_libc_fpstate"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(512) %1, i8* noundef nonnull align 8 dereferenceable(512) %2, i64 512, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::x86_64::user_regs_struct as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN104_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..user_regs_struct$u20$as$u20$core..clone..Clone$GT$5clone17h1204f29698d3e0d3E"(%"unix::linux_like::linux::gnu::b64::x86_64::user_regs_struct"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::x86_64::user_regs_struct") dereferenceable(216) %0, %"unix::linux_like::linux::gnu::b64::x86_64::user_regs_struct"* noalias nocapture readonly align 8 dereferenceable(216) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::user_regs_struct"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::user_regs_struct"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(216) %1, i8* noundef nonnull align 8 dereferenceable(216) %2, i64 216, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::x86_64::user as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN92_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..user$u20$as$u20$core..clone..Clone$GT$5clone17h9003ce7533b31425E"(%"unix::linux_like::linux::gnu::b64::x86_64::user"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::x86_64::user") dereferenceable(912) %0, %"unix::linux_like::linux::gnu::b64::x86_64::user"* noalias nocapture readonly align 8 dereferenceable(912) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::user"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::user"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(912) %1, i8* noundef nonnull align 8 dereferenceable(912) %2, i64 912, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::x86_64::mcontext_t as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN98_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..mcontext_t$u20$as$u20$core..clone..Clone$GT$5clone17h5fae9ad9fc0f3852E"(%"unix::linux_like::linux::gnu::b64::x86_64::mcontext_t"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::x86_64::mcontext_t") dereferenceable(256) %0, %"unix::linux_like::linux::gnu::b64::x86_64::mcontext_t"* noalias nocapture readonly align 8 dereferenceable(256) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::mcontext_t"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::mcontext_t"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(256) %1, i8* noundef nonnull align 8 dereferenceable(256) %2, i64 256, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::x86_64::ipc_perm as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN96_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..ipc_perm$u20$as$u20$core..clone..Clone$GT$5clone17h474f0dc964139c2aE"(%"unix::linux_like::linux::gnu::b64::x86_64::ipc_perm"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::x86_64::ipc_perm") dereferenceable(48) %0, %"unix::linux_like::linux::gnu::b64::x86_64::ipc_perm"* noalias nocapture readonly align 8 dereferenceable(48) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::ipc_perm"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::ipc_perm"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(48) %1, i8* noundef nonnull align 8 dereferenceable(48) %2, i64 48, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::x86_64::shmid_ds as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN96_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..shmid_ds$u20$as$u20$core..clone..Clone$GT$5clone17h80573203cc90a3c2E"(%"unix::linux_like::linux::gnu::b64::x86_64::shmid_ds"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::x86_64::shmid_ds") dereferenceable(112) %0, %"unix::linux_like::linux::gnu::b64::x86_64::shmid_ds"* noalias nocapture readonly align 8 dereferenceable(112) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::shmid_ds"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::shmid_ds"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(112) %1, i8* noundef nonnull align 8 dereferenceable(112) %2, i64 112, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::x86_64::user_fpregs_struct as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN106_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..user_fpregs_struct$u20$as$u20$core..clone..Clone$GT$5clone17h770a8ae6b9f89a8aE"(%"unix::linux_like::linux::gnu::b64::x86_64::user_fpregs_struct"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::x86_64::user_fpregs_struct") dereferenceable(512) %0, %"unix::linux_like::linux::gnu::b64::x86_64::user_fpregs_struct"* noalias nocapture readonly align 8 dereferenceable(512) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::user_fpregs_struct"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::user_fpregs_struct"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(512) %1, i8* noundef nonnull align 8 dereferenceable(512) %2, i64 512, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::x86_64::ucontext_t as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN98_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..ucontext_t$u20$as$u20$core..clone..Clone$GT$5clone17h74f7e8a68c7e1999E"(%"unix::linux_like::linux::gnu::b64::x86_64::ucontext_t"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::x86_64::ucontext_t") dereferenceable(936) %0, %"unix::linux_like::linux::gnu::b64::x86_64::ucontext_t"* noalias nocapture readonly align 8 dereferenceable(936) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::ucontext_t"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::ucontext_t"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(936) %1, i8* noundef nonnull align 8 dereferenceable(936) %2, i64 936, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::x86_64::not_x32::statvfs as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN104_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..not_x32..statvfs$u20$as$u20$core..clone..Clone$GT$5clone17ha213771763e1f6a5E"(%"unix::linux_like::linux::gnu::b64::x86_64::not_x32::statvfs"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::x86_64::not_x32::statvfs") dereferenceable(112) %0, %"unix::linux_like::linux::gnu::b64::x86_64::not_x32::statvfs"* noalias nocapture readonly align 8 dereferenceable(112) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::not_x32::statvfs"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::not_x32::statvfs"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(112) %1, i8* noundef nonnull align 8 dereferenceable(112) %2, i64 112, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::b64::x86_64::align::max_align_t as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN106_$LT$libc..unix..linux_like..linux..gnu..b64..x86_64..align..max_align_t$u20$as$u20$core..clone..Clone$GT$5clone17h7694965a33f21795E"(%"unix::linux_like::linux::gnu::b64::x86_64::align::max_align_t"* noalias nocapture sret(%"unix::linux_like::linux::gnu::b64::x86_64::align::max_align_t") dereferenceable(32) %0, %"unix::linux_like::linux::gnu::b64::x86_64::align::max_align_t"* noalias nocapture readonly align 16 dereferenceable(32) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::align::max_align_t"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::gnu::b64::x86_64::align::max_align_t"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 16 dereferenceable(32) %1, i8* noundef nonnull align 16 dereferenceable(32) %2, i64 32, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::gnu::align::sem_t as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN87_$LT$libc..unix..linux_like..linux..gnu..align..sem_t$u20$as$u20$core..clone..Clone$GT$5clone17h1b09ae81f67ee34aE"(%"unix::linux_like::linux::gnu::align::sem_t"* noalias nocapture sret(%"unix::linux_like::linux::gnu::align::sem_t") dereferenceable(32) %0, %"unix::linux_like::linux::gnu::align::sem_t"* noalias nocapture readonly align 8 dereferenceable(32) %self) unnamed_addr #1 {
start:
  %1 = getelementptr inbounds %"unix::linux_like::linux::gnu::align::sem_t", %"unix::linux_like::linux::gnu::align::sem_t"* %0, i64 0, i32 0, i64 0
  %2 = getelementptr inbounds %"unix::linux_like::linux::gnu::align::sem_t", %"unix::linux_like::linux::gnu::align::sem_t"* %self, i64 0, i32 0, i64 0
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(32) %1, i8* noundef nonnull align 8 dereferenceable(32) %2, i64 32, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::arch::generic::termios2 as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN93_$LT$libc..unix..linux_like..linux..arch..generic..termios2$u20$as$u20$core..clone..Clone$GT$5clone17hb47a6907691042d2E"(%"unix::linux_like::linux::arch::generic::termios2"* noalias nocapture sret(%"unix::linux_like::linux::arch::generic::termios2") dereferenceable(44) %0, %"unix::linux_like::linux::arch::generic::termios2"* noalias nocapture readonly align 4 dereferenceable(44) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::arch::generic::termios2"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::arch::generic::termios2"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 4 dereferenceable(44) %1, i8* noundef nonnull align 4 dereferenceable(44) %2, i64 44, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::pthread_condattr_t as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn
define i32 @"_ZN88_$LT$libc..unix..linux_like..linux..pthread_condattr_t$u20$as$u20$core..clone..Clone$GT$5clone17h83d2a4843a78293eE"(%"unix::linux_like::linux::pthread_condattr_t"* noalias nocapture readonly align 4 dereferenceable(4) %self) unnamed_addr #2 {
start:
  %.sroa.0.0..sroa_cast = bitcast %"unix::linux_like::linux::pthread_condattr_t"* %self to i32*
  %.sroa.0.0.copyload = load i32, i32* %.sroa.0.0..sroa_cast, align 4
  ret i32 %.sroa.0.0.copyload
}

; <libc::unix::linux_like::linux::fanotify_event_metadata as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN93_$LT$libc..unix..linux_like..linux..fanotify_event_metadata$u20$as$u20$core..clone..Clone$GT$5clone17h18c1e9185d4aa166E"(%"unix::linux_like::linux::fanotify_event_metadata"* noalias nocapture sret(%"unix::linux_like::linux::fanotify_event_metadata") dereferenceable(24) %0, %"unix::linux_like::linux::fanotify_event_metadata"* noalias nocapture readonly align 8 dereferenceable(24) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::fanotify_event_metadata"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::fanotify_event_metadata"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(24) %1, i8* noundef nonnull align 8 dereferenceable(24) %2, i64 24, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::pthread_cond_t as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN84_$LT$libc..unix..linux_like..linux..pthread_cond_t$u20$as$u20$core..clone..Clone$GT$5clone17h80246e5c49fcc9cdE"(%"unix::linux_like::linux::pthread_cond_t"* noalias nocapture sret(%"unix::linux_like::linux::pthread_cond_t") dereferenceable(48) %0, %"unix::linux_like::linux::pthread_cond_t"* noalias nocapture readonly align 8 dereferenceable(48) %self) unnamed_addr #1 {
start:
  %1 = getelementptr inbounds %"unix::linux_like::linux::pthread_cond_t", %"unix::linux_like::linux::pthread_cond_t"* %0, i64 0, i32 0, i64 0
  %2 = getelementptr inbounds %"unix::linux_like::linux::pthread_cond_t", %"unix::linux_like::linux::pthread_cond_t"* %self, i64 0, i32 0, i64 0
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(48) %1, i8* noundef nonnull align 8 dereferenceable(48) %2, i64 48, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::pthread_mutex_t as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN85_$LT$libc..unix..linux_like..linux..pthread_mutex_t$u20$as$u20$core..clone..Clone$GT$5clone17h3785430c4d9a38fcE"(%"unix::linux_like::linux::pthread_mutex_t"* noalias nocapture sret(%"unix::linux_like::linux::pthread_mutex_t") dereferenceable(40) %0, %"unix::linux_like::linux::pthread_mutex_t"* noalias nocapture readonly align 8 dereferenceable(40) %self) unnamed_addr #1 {
start:
  %1 = getelementptr inbounds %"unix::linux_like::linux::pthread_mutex_t", %"unix::linux_like::linux::pthread_mutex_t"* %0, i64 0, i32 0, i64 0
  %2 = getelementptr inbounds %"unix::linux_like::linux::pthread_mutex_t", %"unix::linux_like::linux::pthread_mutex_t"* %self, i64 0, i32 0, i64 0
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(40) %1, i8* noundef nonnull align 8 dereferenceable(40) %2, i64 40, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::pthread_rwlock_t as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN86_$LT$libc..unix..linux_like..linux..pthread_rwlock_t$u20$as$u20$core..clone..Clone$GT$5clone17h415404e44f3874a7E"(%"unix::linux_like::linux::pthread_rwlock_t"* noalias nocapture sret(%"unix::linux_like::linux::pthread_rwlock_t") dereferenceable(56) %0, %"unix::linux_like::linux::pthread_rwlock_t"* noalias nocapture readonly align 8 dereferenceable(56) %self) unnamed_addr #1 {
start:
  %1 = getelementptr inbounds %"unix::linux_like::linux::pthread_rwlock_t", %"unix::linux_like::linux::pthread_rwlock_t"* %0, i64 0, i32 0, i64 0
  %2 = getelementptr inbounds %"unix::linux_like::linux::pthread_rwlock_t", %"unix::linux_like::linux::pthread_rwlock_t"* %self, i64 0, i32 0, i64 0
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(56) %1, i8* noundef nonnull align 8 dereferenceable(56) %2, i64 56, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::canfd_frame as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN81_$LT$libc..unix..linux_like..linux..canfd_frame$u20$as$u20$core..clone..Clone$GT$5clone17h532d2e57f8380159E"(%"unix::linux_like::linux::canfd_frame"* noalias nocapture sret(%"unix::linux_like::linux::canfd_frame") dereferenceable(72) %0, %"unix::linux_like::linux::canfd_frame"* noalias nocapture readonly align 8 dereferenceable(72) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::canfd_frame"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::canfd_frame"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(72) %1, i8* noundef nonnull align 8 dereferenceable(72) %2, i64 72, i1 false)
  ret void
}

; <libc::unix::linux_like::linux::non_exhaustive::open_how as core::clone::Clone>::clone
; Function Attrs: mustprogress nofree nosync nounwind nonlazybind uwtable willreturn
define void @"_ZN94_$LT$libc..unix..linux_like..linux..non_exhaustive..open_how$u20$as$u20$core..clone..Clone$GT$5clone17he441963c1a6eb2a7E"(%"unix::linux_like::linux::non_exhaustive::open_how"* noalias nocapture sret(%"unix::linux_like::linux::non_exhaustive::open_how") dereferenceable(24) %0, %"unix::linux_like::linux::non_exhaustive::open_how"* noalias nocapture readonly align 8 dereferenceable(24) %self) unnamed_addr #1 {
start:
  %1 = bitcast %"unix::linux_like::linux::non_exhaustive::open_how"* %0 to i8*
  %2 = bitcast %"unix::linux_like::linux::non_exhaustive::open_how"* %self to i8*
  tail call void @llvm.memcpy.p0i8.p0i8.i64(i8* noundef nonnull align 8 dereferenceable(24) %1, i8* noundef nonnull align 8 dereferenceable(24) %2, i64 24, i1 false)
  ret void
}

; Function Attrs: cold noreturn nounwind
declare void @llvm.trap() #3

; Function Attrs: argmemonly mustprogress nofree nounwind willreturn
declare void @llvm.memcpy.p0i8.p0i8.i64(i8* noalias nocapture writeonly, i8* noalias nocapture readonly, i64, i1 immarg) #4

attributes #0 = { noreturn nounwind nonlazybind uwtable "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #1 = { mustprogress nofree nosync nounwind nonlazybind uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #2 = { mustprogress nofree norecurse nosync nounwind nonlazybind readonly uwtable willreturn "probe-stack"="__rust_probestack" "target-cpu"="x86-64" }
attributes #3 = { cold noreturn nounwind }
attributes #4 = { argmemonly mustprogress nofree nounwind willreturn }

!llvm.module.flags = !{!0, !1}

!0 = !{i32 7, !"PIC Level", i32 2}
!1 = !{i32 2, !"RtLibUseGOT", i32 1}
