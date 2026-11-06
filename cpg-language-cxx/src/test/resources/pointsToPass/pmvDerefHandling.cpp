undefined1 sgx_t_ie(u64 *param_1)

{
  u64 uVar1;
  size_t len;
  int iVar2;
  u8 *path;
  
    if (uVar1 == 0) {
      sgx_tstd::rt::t_ie(*param_1,(u8 *)0x0,len);
      return false;
    }
    iVar2 = sgx_is_outside_enclave(uVar1);
    if (iVar2 != 0) {
	  uVar1 = param_1[1];
      if (len == 0) {
        sgx_tstd::rt::t_ie(*param_1,(u8 *)0x0,0);
        return false;
      }
      path = (u8 *)dlmalloc(len);
      if (path == (u8 *)0x0) {
        return 3;
      }
      iVar2 = memcpy_s(path,len,uVar1,len);
      if (iVar2 == 0) {
        sgx_tstd::rt::t_ie(*param_1,path,len);
      }
      dlfree(path);
      return iVar2 != 0;
    }
  return 2;
}




// EXTRACTED ECALL

undefined1 sgx_cw(i32 *param_1)

{
  long lVar1;
  c_void *session;
  undefined1 uVar2;
  int iVar3;
  int iVar4;
  i32 iVar5;
  i8 *buf;
  size_t __size;
  
        iVar4 = memcpy_s(buf,__size,lVar1,__size);
        if (iVar4 == 0) {
          iVar5 = client::client::cw(session,buf,iVar3);
          *param_1 = iVar5;
        }
        dlfree(buf);
        return iVar4 != 0;
      }
   }
    iVar5 = client::client::cw(session,(i8 *)0x0,iVar3);
}



// EXTRACTED ECALL

undefined1 sgx_read(i32 *param_1)

{
  u8 *lVar1;
  void *session;
  undefined1 uVar2;
  int iVar3;
  int iVar4;
  i32 iVar5;
  u64 buf;
  size_t __nmemb;
  
  buf = *param_1;
  iVar3 = memcpy_s(lVar1,__nmemb,buf,__nmemb);
  dlfree(buf);
  
  iVar5 = read(lVar1);
  return uVar2;
}

void read(void* l) {
  println(*l);
}

// WARNING: Removing unreachable block (ram,0x00134999)

void dlfree(void *__ptr)

{
  uint *puVar6;
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
      *puVar6 = 1;
  return;
}

