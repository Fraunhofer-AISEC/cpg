#include "export.h"
#include "globals.h"




// EXTRACTED ECALL

undefined1 sgx_t_global_init_ecall(u64 *param_1)

{
  u64 uVar1;
  size_t len;
  int iVar2;
  u8 *path;
  
  if (param_1 == (u64 *)0x0) {
    return 2;
  }
  iVar2 = sgx_is_outside_enclave();
  if (iVar2 != 0) {
    uVar1 = param_1[1];
    len = param_1[2];
    if (uVar1 == 0) {
      sgx_tstd::rt::t_global_init_ecall(*param_1,(u8 *)0x0,len);
      return false;
    }
    iVar2 = sgx_is_outside_enclave(uVar1);
    if (iVar2 != 0) {
      if (len == 0) {
        sgx_tstd::rt::t_global_init_ecall(*param_1,(u8 *)0x0,0);
        return false;
      }
      path = (u8 *)dlmalloc(len);
      if (path == (u8 *)0x0) {
        return 3;
      }
      iVar2 = memcpy_s(path,len,uVar1,len);
      if (iVar2 == 0) {
        sgx_tstd::rt::t_global_init_ecall(*param_1,path,len);
      }
      dlfree(path);
      return iVar2 != 0;
    }
  }
  return 2;
}



// EXTRACTED ECALL
/*
undefined8 sgx_tls_client_wants_read(i32 *param_1)

{
  int iVar1;
  i32 iVar2;
  
  if (param_1 == (i32 *)0x0) {
    return 2;
  }
  iVar1 = sgx_is_outside_enclave(param_1,0x10);
  if (iVar1 == 0) {
    return 2;
  }
  iVar2 = tlsclient::tlsclient::tls_client_wants_read(*(c_void **)(param_1 + 2));
  *param_1 = iVar2;
  return 0;
}
*/


// EXTRACTED ECALL

undefined1 sgx_tls_client_write(i32 *param_1)

{
  long lVar1;
  c_void *session;
  undefined1 uVar2;
  int iVar3;
  int iVar4;
  i32 iVar5;
  i8 *buf;
  size_t __size;
  
  if (param_1 == (i32 *)0x0) {
    return 2;
  }
  iVar3 = sgx_is_outside_enclave();
  if (iVar3 == 0) {
LAB_00127e14:
    uVar2 = 2;
  }
  else {
    lVar1 = *(long *)(param_1 + 4);
    iVar3 = param_1[6];
    __size = (size_t)iVar3;
    session = *(c_void **)(param_1 + 2);
    if (lVar1 != 0) {
      iVar4 = sgx_is_outside_enclave(lVar1);
      if (iVar4 == 0) goto LAB_00127e14;
      if (__size != 0) {
        buf = (i8 *)dlmalloc(__size);
        if (buf == (i8 *)0x0) {
          return 3;
        }
        iVar4 = memcpy_s(buf,__size,lVar1,__size);
        if (iVar4 == 0) {
          iVar5 = tlsclient::tlsclient::tls_client_write(session,buf,iVar3);
          *param_1 = iVar5;
        }
        dlfree(buf);
        return iVar4 != 0;
      }
    }
    iVar5 = tlsclient::tlsclient::tls_client_write(session,(i8 *)0x0,iVar3);
    *param_1 = iVar5;
    uVar2 = 0;
  }
  return uVar2;
}



// EXTRACTED ECALL

undefined1 sgx_tls_client_read(i32 *param_1)

{
  long lVar1;
  c_void *session;
  undefined1 uVar2;
  int iVar3;
  int iVar4;
  i32 iVar5;
  i8 *buf;
  size_t __nmemb;
  
  if (param_1 == (i32 *)0x0) {
    return 2;
  }
  iVar3 = sgx_is_outside_enclave();
  if (iVar3 == 0) {
LAB_00127f04:
    uVar2 = 2;
  }
  else {
    lVar1 = *(long *)(param_1 + 4);
    iVar3 = param_1[6];
    __nmemb = (size_t)iVar3;
    session = *(c_void **)(param_1 + 2);
    if (lVar1 != 0) {
      iVar4 = sgx_is_outside_enclave(lVar1);
      if (iVar4 == 0) goto LAB_00127f04;
      if (__nmemb != 0) {
        buf = (i8 *)dlcalloc(__nmemb,1);
        if (buf == (i8 *)0x0) {
          return 3;
        }
        iVar5 = tlsclient::tlsclient::tls_client_read(session,buf,iVar3);
        *param_1 = iVar5;
        iVar3 = memcpy_s(lVar1,__nmemb,buf,__nmemb);
        dlfree(buf);
        return iVar3 != 0;
      }
    }
    iVar5 = tlsclient::tlsclient::tls_client_read(session,(i8 *)0x0,iVar3);
    *param_1 = iVar5;
    uVar2 = 0;
  }
  return uVar2;
}

// WARNING: Removing unreachable block (ram,0x00134999)

void dlfree(void *__ptr)

{
  uint *puVar1;
  long lVar2;
  code *pcVar3;
  uint uVar4;
  uint uVar5;
  uint *puVar6;
  uint *puVar7;
  long lVar8;
  uint *puVar9;
  void *pvVar10;
  undefined1 *puVar11;
  void *pvVar12;
  byte bVar13;
  ulong uVar14;
  uint *puVar15;
  ulong uVar16;
  uint *puVar17;
  uint *puVar18;
  uint uVar19;
  ulong uVar20;
  uint *puVar21;
  uint *puVar22;
  long lVar23;
  
  if (__ptr == (void *)0x0) {
    return;
  }
  puVar18 = (uint *)((long)__ptr + -0x10);
  puVar17 = (uint *)(mparams ^
                    *(ulong *)((long)__ptr +
                              ((*(ulong *)((long)__ptr + -8) & 0xfffffffffffffff8) - 0x10)));
  if (mparams != *(ulong *)(puVar17 + 0x10)) goto LAB_00134510;
  if ((puVar17[0xdc] & 2) != 0) {
    puVar6 = puVar17 + 0xdd;
    LOCK();
    uVar4 = *puVar6;
    *puVar6 = 1;
    UNLOCK();
    while (uVar4 != 0) {
      do {
      } while (puVar17[0xdd] != 0);
      LOCK();
      uVar4 = *puVar6;
      *puVar6 = 1;
      UNLOCK();
    }
  }
  return;
}

