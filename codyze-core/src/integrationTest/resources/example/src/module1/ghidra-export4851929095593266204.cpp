#include "ghidra-export4851929095593266204.h"



void _DT_INIT(void)

{
  __gmon_start__();
  return;
}



void FUN_00101020(void)

{
  (*(code *)(undefined *)0x0)();
  return;
}



// WARNING: Unknown calling convention -- yet parameter storage is locked

size_t fread(void *__ptr,size_t __size,size_t __n,FILE *__stream)

{
  size_t sVar1;
  
  sVar1 = fread(__ptr,__size,__n,__stream);
  return sVar1;
}



void __stack_chk_fail(void)

{
                    // WARNING: Subroutine does not return
  __stack_chk_fail();
}



// WARNING: Unknown calling convention -- yet parameter storage is locked

int feof(FILE *__stream)

{
  int iVar1;
  
  iVar1 = feof(__stream);
  return iVar1;
}



// WARNING: Unknown calling convention -- yet parameter storage is locked

size_t fwrite(void *__ptr,size_t __size,size_t __n,FILE *__s)

{
  size_t sVar1;
  
  sVar1 = fwrite(__ptr,__size,__n,__s);
  return sVar1;
}



undefined8 FUN_00101070(void)

{
  int iVar1;
  long in_FS_OFFSET;
  undefined1 auStack_f8 [192];
  undefined1 local_38 [24];
  long local_20;
  
  local_20 = *(long *)(in_FS_OFFSET + 0x28);
  FUN_00101240();
  fread(auStack_f8,4,4,stdin);
  while( true ) {
    iVar1 = feof(stdin);
    if (iVar1 != 0) break;
    fread(local_38,4,4,stdin);
    iVar1 = feof(stdin);
    if (iVar1 == 0) {
      FUN_00101740(auStack_f8);
      fwrite(local_38,4,4,stdout);
    }
  }
  if (local_20 == *(long *)(in_FS_OFFSET + 0x28)) {
    return 0;
  }
                    // WARNING: Subroutine does not return
  __stack_chk_fail();
}



void processEntry entry(undefined8 param_1,undefined8 param_2)

{
  undefined1 auStack_8 [8];
  
  __libc_start_main(FUN_00101070,param_2,&stack0x00000008,0,0,param_1,auStack_8);
  do {
                    // WARNING: Do nothing block with infinite loop
  } while( true );
}



// WARNING: Removing unreachable block (ram,0x00101183)
// WARNING: Removing unreachable block (ram,0x0010118f)

void FUN_00101170(void)

{
  return;
}



// WARNING: Removing unreachable block (ram,0x001011c4)
// WARNING: Removing unreachable block (ram,0x001011d0)

void FUN_001011a0(void)

{
  return;
}



void _FINI_0(void)

{
  if (DAT_00104358 != '\0') {
    return;
  }
  __cxa_finalize(PTR_LOOP_00104028);
  FUN_00101170();
  DAT_00104358 = 1;
  return;
}



void _INIT_0(void)

{
  FUN_001011a0();
  return;
}



void FUN_00101240(void)

{
  FUN_00101c50(&DAT_00104360);
  return;
}



void FUN_00101250(undefined8 *param_1)

{
  uint uVar1;
  uint *puVar2;
  uint uVar3;
  uint *puVar4;
  int iVar5;
  long lVar6;
  
  puVar4 = (uint *)(param_1 + 2);
  uVar3 = 4;
  param_1[2] = *param_1;
  param_1[3] = param_1[1];
  iVar5 = 1;
  do {
    uVar1 = puVar4[3];
    puVar2 = puVar4;
    while ((uVar3 & 3) != 0) {
      uVar3 = uVar3 + 1;
      puVar2[4] = *puVar2 ^ uVar1;
      if (uVar3 == 0x2c) {
        return;
      }
      uVar1 = puVar2[4];
      puVar2 = puVar2 + 1;
    }
    lVar6 = (long)iVar5;
    uVar3 = uVar3 + 1;
    puVar4 = puVar2 + 1;
    iVar5 = iVar5 + 1;
    puVar2[4] = CONCAT31(CONCAT21(CONCAT11((&DAT_00104360)[uVar1 & 0xff],
                                           (&DAT_00104360)[uVar1 >> 0x18]),
                                  (&DAT_00104360)[uVar1 >> 0x10 & 0xff]),
                         (&DAT_00104240)[lVar6] ^ (&DAT_00104360)[uVar1 >> 8 & 0xff]) ^ *puVar2;
  } while( true );
}



void FUN_00101320(long param_1,int param_2)

{
  ulong *puVar1;
  ulong uVar2;
  
  puVar1 = (ulong *)(param_1 + 0x10 + (long)param_2 * 0x10);
  uVar2 = puVar1[1];
  *(ulong *)(param_1 + 0xc0) = *(ulong *)(param_1 + 0xc0) ^ *puVar1;
  *(ulong *)(param_1 + 200) = *(ulong *)(param_1 + 200) ^ uVar2;
  return;
}



void FUN_00101340(byte *param_1)

{
  *param_1 = (&DAT_00104360)[*param_1];
  param_1[1] = (&DAT_00104360)[param_1[1]];
  param_1[2] = (&DAT_00104360)[param_1[2]];
  param_1[3] = (&DAT_00104360)[param_1[3]];
  param_1[4] = (&DAT_00104360)[param_1[4]];
  param_1[5] = (&DAT_00104360)[param_1[5]];
  param_1[6] = (&DAT_00104360)[param_1[6]];
  param_1[7] = (&DAT_00104360)[param_1[7]];
  param_1[8] = (&DAT_00104360)[param_1[8]];
  param_1[9] = (&DAT_00104360)[param_1[9]];
  param_1[10] = (&DAT_00104360)[param_1[10]];
  param_1[0xb] = (&DAT_00104360)[param_1[0xb]];
  param_1[0xc] = (&DAT_00104360)[param_1[0xc]];
  param_1[0xd] = (&DAT_00104360)[param_1[0xd]];
  param_1[0xe] = (&DAT_00104360)[param_1[0xe]];
  param_1[0xf] = (&DAT_00104360)[param_1[0xf]];
  return;
}



void FUN_00101400(long param_1)

{
  undefined1 uVar1;
  undefined1 uVar2;
  
  uVar1 = *(undefined1 *)(param_1 + 5);
  uVar2 = *(undefined1 *)(param_1 + 0xd);
  *(ushort *)(param_1 + 5) = CONCAT11(*(undefined1 *)(param_1 + 0xe),*(undefined1 *)(param_1 + 9));
  *(ushort *)(param_1 + 0xd) = CONCAT11(*(undefined1 *)(param_1 + 6),*(undefined1 *)(param_1 + 1));
  *(ushort *)(param_1 + 1) = CONCAT11(*(undefined1 *)(param_1 + 10),uVar1);
  *(ushort *)(param_1 + 9) = CONCAT11(*(undefined1 *)(param_1 + 2),uVar2);
  uVar1 = *(undefined1 *)(param_1 + 0xb);
  *(undefined1 *)(param_1 + 0xb) = *(undefined1 *)(param_1 + 7);
  *(undefined1 *)(param_1 + 7) = *(undefined1 *)(param_1 + 3);
  uVar2 = *(undefined1 *)(param_1 + 0xf);
  *(undefined1 *)(param_1 + 0xf) = uVar1;
  *(undefined1 *)(param_1 + 3) = uVar2;
  return;
}



void FUN_00101450(byte *param_1)

{
  byte bVar1;
  byte bVar2;
  byte bVar3;
  byte bVar4;
  byte bVar5;
  byte bVar6;
  byte bVar7;
  byte bVar8;
  byte bVar9;
  byte bVar10;
  byte bVar11;
  byte bVar12;
  byte bVar13;
  byte bVar14;
  byte bVar15;
  byte bVar16;
  byte bVar17;
  byte bVar18;
  byte bVar19;
  byte bVar20;
  byte bVar21;
  byte bVar22;
  byte bVar23;
  byte bVar24;
  byte bVar25;
  byte bVar26;
  byte bVar27;
  byte bVar28;
  byte bVar29;
  byte bVar30;
  byte bVar31;
  
  bVar9 = *param_1;
  bVar10 = param_1[1];
  bVar11 = param_1[3];
  bVar12 = param_1[2];
  bVar13 = param_1[4];
  bVar14 = param_1[5];
  bVar15 = param_1[6];
  bVar16 = param_1[7];
  bVar17 = param_1[9];
  bVar18 = param_1[8];
  bVar19 = param_1[0xb];
  bVar20 = (&DAT_00104140)[bVar18];
  bVar1 = (&DAT_00104040)[bVar17];
  bVar21 = (&DAT_00104140)[bVar17];
  bVar2 = (&DAT_00104040)[param_1[10]];
  bVar22 = (&DAT_00104140)[param_1[10]];
  bVar3 = (&DAT_00104040)[bVar19];
  bVar23 = param_1[0xe];
  bVar24 = param_1[0xc];
  bVar25 = (&DAT_00104040)[bVar18];
  bVar4 = (&DAT_00104140)[bVar19];
  bVar26 = param_1[0xd];
  bVar27 = param_1[0xf];
  bVar28 = (&DAT_00104140)[bVar24];
  bVar5 = (&DAT_00104040)[bVar26];
  bVar29 = (&DAT_00104140)[bVar26];
  bVar6 = (&DAT_00104040)[bVar23];
  bVar30 = (&DAT_00104140)[bVar23];
  bVar7 = (&DAT_00104040)[bVar27];
  bVar31 = (&DAT_00104040)[bVar24];
  bVar8 = (&DAT_00104140)[bVar27];
  *(ulong *)param_1 =
       CONCAT17(bVar15 ^ bVar14 ^ (&DAT_00104140)[bVar16] ^ (&DAT_00104040)[bVar13],
                CONCAT16(bVar13 ^ bVar14 ^ (&DAT_00104040)[bVar16] ^ (&DAT_00104140)[bVar15],
                         CONCAT15((&DAT_00104040)[bVar15] ^ (&DAT_00104140)[bVar14] ^
                                  bVar16 ^ bVar13,
                                  CONCAT14((&DAT_00104040)[bVar14] ^ (&DAT_00104140)[bVar13] ^
                                           bVar16 ^ bVar15,
                                           CONCAT13((&DAT_00104140)[bVar11] ^ (&DAT_00104040)[bVar9]
                                                    ^ bVar12 ^ bVar10,
                                                    CONCAT12((&DAT_00104040)[bVar11] ^
                                                             (&DAT_00104140)[bVar12] ^
                                                             bVar9 ^ bVar10,
                                                             CONCAT11((&DAT_00104040)[bVar12] ^
                                                                      (&DAT_00104140)[bVar10] ^
                                                                      bVar11 ^ bVar9,
                                                                      (&DAT_00104040)[bVar10] ^
                                                                      (&DAT_00104140)[bVar9] ^
                                                                      bVar11 ^ bVar12)))))));
  *(ulong *)(param_1 + 8) =
       CONCAT17(bVar26 ^ bVar23 ^ bVar31 ^ bVar8,
                CONCAT16(bVar24 ^ bVar26 ^ bVar30 ^ bVar7,
                         CONCAT15(bVar29 ^ bVar6 ^ bVar24 ^ bVar27,
                                  CONCAT14(bVar28 ^ bVar5 ^ bVar23 ^ bVar27,
                                           CONCAT13(bVar17 ^ param_1[10] ^ bVar25 ^ bVar4,
                                                    CONCAT12(bVar22 ^ bVar3 ^ bVar18 ^ bVar17,
                                                             CONCAT11(bVar18 ^ bVar19 ^
                                                                      bVar21 ^ bVar2,
                                                                      bVar20 ^ bVar1 ^
                                                                      param_1[10] ^ bVar19)))))));
  return;
}



void FUN_00101740(long param_1)

{
  undefined1 uVar1;
  undefined1 uVar2;
  undefined1 uVar3;
  undefined1 uVar4;
  undefined1 uVar5;
  undefined1 uVar6;
  undefined1 uVar7;
  undefined1 uVar8;
  undefined1 uVar9;
  undefined1 uVar10;
  undefined1 uVar11;
  undefined1 uVar12;
  undefined1 auVar13 [16];
  ulong uVar14;
  ulong uVar15;
  undefined1 (*pauVar16) [16];
  undefined1 (*pauVar17) [16];
  ulong uVar18;
  ulong uVar19;
  ulong uVar20;
  uint uVar21;
  ulong uVar22;
  ulong uVar23;
  ulong uVar24;
  uint uVar25;
  ulong uVar26;
  uint uVar27;
  ulong uVar28;
  ulong uVar29;
  byte local_4d;
  byte local_4c;
  byte local_4b;
  byte local_4a;
  byte local_49;
  
  FUN_00101250();
  auVar13 = *(undefined1 (*) [16])(param_1 + 0xc0) ^ *(undefined1 (*) [16])(param_1 + 0x10);
  vpextrb_avx(auVar13,0xc);
  vpextrb_avx(auVar13,0xd);
  *(undefined1 (*) [16])(param_1 + 0xc0) = auVar13;
  uVar25 = vpextrb_avx(auVar13,0);
  uVar26 = (ulong)uVar25;
  vpextrb_avx(auVar13,0xe);
  vpextrb_avx(auVar13,0xf);
  vpextrb_avx(auVar13,9);
  uVar25 = vpextrb_avx(auVar13,1);
  uVar24 = (ulong)uVar25;
  uVar25 = vpextrb_avx(auVar13,2);
  uVar23 = (ulong)uVar25;
  uVar25 = vpextrb_avx(auVar13,3);
  uVar22 = (ulong)uVar25;
  uVar25 = vpextrb_avx(auVar13,4);
  uVar20 = (ulong)uVar25;
  uVar25 = vpextrb_avx(auVar13,5);
  uVar19 = (ulong)uVar25;
  uVar25 = vpextrb_avx(auVar13,6);
  uVar18 = (ulong)uVar25;
  uVar25 = vpextrb_avx(auVar13,7);
  uVar14 = (ulong)uVar25;
  uVar25 = vpextrb_avx(auVar13,8);
  uVar15 = (ulong)uVar25;
  uVar25 = vpextrb_avx(auVar13,10);
  uVar29 = (ulong)uVar25;
  uVar25 = vpextrb_avx(auVar13,0xb);
  uVar28 = (ulong)uVar25;
  pauVar16 = (undefined1 (*) [16])(param_1 + 0x20);
  do {
    pauVar17 = pauVar16 + 1;
    *(undefined *)(param_1 + 0xc0) = (&DAT_00104360)[uVar26 & 0xff];
    uVar1 = (&DAT_00104360)[uVar24 & 0xff];
    *(undefined1 *)(param_1 + 0xc1) = uVar1;
    uVar2 = (&DAT_00104360)[uVar23 & 0xff];
    *(undefined1 *)(param_1 + 0xc2) = uVar2;
    uVar3 = (&DAT_00104360)[uVar22 & 0xff];
    *(undefined1 *)(param_1 + 0xc3) = uVar3;
    *(undefined *)(param_1 + 0xc4) = (&DAT_00104360)[uVar20 & 0xff];
    uVar4 = (&DAT_00104360)[uVar19 & 0xff];
    *(undefined1 *)(param_1 + 0xc5) = uVar4;
    uVar5 = (&DAT_00104360)[uVar18 & 0xff];
    *(undefined1 *)(param_1 + 0xc6) = uVar5;
    uVar6 = (&DAT_00104360)[uVar14 & 0xff];
    *(undefined1 *)(param_1 + 199) = uVar6;
    *(undefined *)(param_1 + 200) = (&DAT_00104360)[uVar15 & 0xff];
    uVar7 = (&DAT_00104360)[local_49];
    *(undefined1 *)(param_1 + 0xc9) = uVar7;
    uVar8 = (&DAT_00104360)[uVar29 & 0xff];
    *(undefined1 *)(param_1 + 0xca) = uVar8;
    uVar9 = (&DAT_00104360)[uVar28 & 0xff];
    *(undefined1 *)(param_1 + 0xcb) = uVar9;
    *(undefined *)(param_1 + 0xcc) = (&DAT_00104360)[local_4d];
    uVar10 = (&DAT_00104360)[local_4c];
    *(undefined1 *)(param_1 + 0xcd) = uVar10;
    uVar11 = (&DAT_00104360)[local_4b];
    *(undefined1 *)(param_1 + 0xce) = uVar11;
    uVar12 = (&DAT_00104360)[local_4a];
    *(undefined1 *)(param_1 + 0xc1) = uVar4;
    *(undefined1 *)(param_1 + 0xc5) = uVar7;
    *(undefined1 *)(param_1 + 0xc9) = uVar10;
    *(undefined1 *)(param_1 + 0xcd) = uVar1;
    *(undefined1 *)(param_1 + 0xc6) = uVar11;
    *(undefined1 *)(param_1 + 0xce) = uVar5;
    *(undefined1 *)(param_1 + 0xc2) = uVar8;
    *(undefined1 *)(param_1 + 0xca) = uVar2;
    *(undefined1 *)(param_1 + 0xcb) = uVar6;
    *(undefined1 *)(param_1 + 199) = uVar3;
    *(undefined1 *)(param_1 + 0xc3) = uVar12;
    *(undefined1 *)(param_1 + 0xcf) = uVar9;
    FUN_00101450(param_1 + 0xc0);
    local_49 = *(byte *)(param_1 + 0xc9) ^ (*pauVar16)[9];
    auVar13 = *pauVar16 ^ *(undefined1 (*) [16])(param_1 + 0xc0);
    local_4d = *(byte *)(param_1 + 0xcc) ^ (*pauVar16)[0xc];
    uVar26 = (ulong)(*(byte *)(param_1 + 0xc0) ^ (*pauVar16)[0]);
    uVar24 = (ulong)(*(byte *)(param_1 + 0xc1) ^ (*pauVar16)[1]);
    local_4c = *(byte *)(param_1 + 0xcd) ^ (*pauVar16)[0xd];
    uVar23 = (ulong)(*(byte *)(param_1 + 0xc2) ^ (*pauVar16)[2]);
    uVar22 = (ulong)(*(byte *)(param_1 + 0xc3) ^ (*pauVar16)[3]);
    uVar20 = (ulong)(*(byte *)(param_1 + 0xc4) ^ (*pauVar16)[4]);
    uVar19 = (ulong)(*(byte *)(param_1 + 0xc5) ^ (*pauVar16)[5]);
    uVar18 = (ulong)(*(byte *)(param_1 + 0xc6) ^ (*pauVar16)[6]);
    uVar14 = (ulong)(*(byte *)(param_1 + 199) ^ (*pauVar16)[7]);
    uVar15 = (ulong)(*(byte *)(param_1 + 200) ^ (*pauVar16)[8]);
    uVar29 = (ulong)(*(byte *)(param_1 + 0xca) ^ (*pauVar16)[10]);
    uVar28 = (ulong)(*(byte *)(param_1 + 0xcb) ^ (*pauVar16)[0xb]);
    local_4b = *(byte *)(param_1 + 0xce) ^ (*pauVar16)[0xe];
    local_4a = *(byte *)(param_1 + 0xcf) ^ (*pauVar16)[0xf];
    *(undefined1 (*) [16])(param_1 + 0xc0) = auVar13;
    pauVar16 = pauVar17;
  } while ((undefined1 (*) [16])(param_1 + 0xb0) != pauVar17);
  uVar25 = vpextrb_avx(auVar13,0);
  uVar21 = vpextrb_avx(auVar13,0xc);
  uVar27 = vpextrb_avx(auVar13,0xf);
  *(undefined *)(param_1 + 0xc0) = (&DAT_00104360)[uVar25];
  uVar25 = vpextrb_avx(auVar13,1);
  uVar1 = (&DAT_00104360)[uVar25];
  uVar25 = vpextrb_avx(auVar13,2);
  *(undefined1 *)(param_1 + 0xc1) = uVar1;
  uVar2 = (&DAT_00104360)[uVar25];
  uVar25 = vpextrb_avx(auVar13,3);
  *(undefined1 *)(param_1 + 0xc2) = uVar2;
  uVar3 = (&DAT_00104360)[uVar25];
  uVar25 = vpextrb_avx(auVar13,4);
  *(undefined1 *)(param_1 + 0xc3) = uVar3;
  *(undefined *)(param_1 + 0xc4) = (&DAT_00104360)[uVar25];
  uVar25 = vpextrb_avx(auVar13,5);
  uVar4 = (&DAT_00104360)[uVar25];
  uVar25 = vpextrb_avx(auVar13,6);
  *(undefined1 *)(param_1 + 0xc5) = uVar4;
  uVar5 = (&DAT_00104360)[uVar25];
  uVar25 = vpextrb_avx(auVar13,7);
  *(undefined1 *)(param_1 + 0xc6) = uVar5;
  uVar6 = (&DAT_00104360)[uVar25];
  uVar25 = vpextrb_avx(auVar13,8);
  *(undefined1 *)(param_1 + 199) = uVar6;
  *(undefined *)(param_1 + 200) = (&DAT_00104360)[uVar25];
  uVar25 = vpextrb_avx(auVar13,9);
  uVar7 = (&DAT_00104360)[uVar25];
  uVar25 = vpextrb_avx(auVar13,10);
  *(undefined1 *)(param_1 + 0xc9) = uVar7;
  uVar8 = (&DAT_00104360)[uVar25];
  uVar25 = vpextrb_avx(auVar13,0xb);
  *(undefined1 *)(param_1 + 0xca) = uVar8;
  uVar9 = (&DAT_00104360)[uVar25];
  *(undefined1 *)(param_1 + 0xcb) = uVar9;
  *(undefined *)(param_1 + 0xcc) = (&DAT_00104360)[uVar21];
  uVar25 = vpextrb_avx(auVar13,0xd);
  uVar10 = (&DAT_00104360)[uVar25];
  uVar25 = vpextrb_avx(auVar13,0xe);
  *(undefined1 *)(param_1 + 0xcd) = uVar10;
  uVar11 = (&DAT_00104360)[uVar25];
  *(undefined1 *)(param_1 + 0xce) = uVar11;
  uVar12 = (&DAT_00104360)[uVar27];
  *(undefined1 *)(param_1 + 0xc1) = uVar4;
  *(undefined1 *)(param_1 + 0xc5) = uVar7;
  *(undefined1 *)(param_1 + 0xc9) = uVar10;
  *(undefined1 *)(param_1 + 0xcd) = uVar1;
  *(undefined1 *)(param_1 + 0xc6) = uVar11;
  *(undefined1 *)(param_1 + 0xce) = uVar5;
  *(undefined1 *)(param_1 + 0xc2) = uVar8;
  *(undefined1 *)(param_1 + 0xca) = uVar2;
  *(undefined1 *)(param_1 + 0xcb) = uVar6;
  *(undefined1 *)(param_1 + 199) = uVar3;
  *(undefined1 *)(param_1 + 0xcf) = uVar9;
  *(undefined1 *)(param_1 + 0xc3) = uVar12;
  *(ulong *)(param_1 + 0xc0) = *(ulong *)(param_1 + 0xc0) ^ *(ulong *)(param_1 + 0xb0);
  *(ulong *)(param_1 + 200) = *(ulong *)(param_1 + 200) ^ *(ulong *)(param_1 + 0xb8);
  return;
}



// WARNING: Globals starting with '_' overlap smaller symbols at the same address

void FUN_00101bd0(uint *param_1,int param_2,undefined8 param_3,undefined8 param_4)

{
  byte bVar1;
  undefined4 uVar2;
  undefined8 in_RAX;
  uint uVar3;
  undefined1 auVar4 [16];
  
  uVar3 = *param_1;
  auVar4 = vpshufb_avx(ZEXT416(uVar3),_DAT_00102010);
  uVar2 = vpextrb_avx(ZEXT416(uVar3),1);
  *param_1 = auVar4._0_4_;
  bVar1 = (&DAT_00104360)[CONCAT44((int)((ulong)in_RAX >> 0x20),uVar2)];
  auVar4 = ZEXT416(uVar3);
  uVar2 = vpextrb_avx(auVar4,2);
  *(byte *)param_1 = bVar1;
  *(undefined *)((long)param_1 + 1) = (&DAT_00104360)[CONCAT44((int)((ulong)param_4 >> 0x20),uVar2)]
  ;
  uVar3 = vpextrb_avx(auVar4,3);
  *(undefined *)((long)param_1 + 2) = (&DAT_00104360)[uVar3];
  uVar3 = vpextrb_avx(auVar4,0);
  *(undefined *)((long)param_1 + 3) = (&DAT_00104360)[uVar3];
  *(byte *)param_1 = bVar1 ^ (&DAT_00104240)[param_2];
  return;
}



// WARNING: Globals starting with '_' overlap smaller symbols at the same address

void FUN_00101c30(uint *param_1)

{
  undefined1 auVar1 [16];
  
  auVar1 = vpshufb_avx(ZEXT416(*param_1),_DAT_00102010);
  *param_1 = auVar1._0_4_;
  return;
}



void FUN_00101c50(undefined1 *param_1)

{
  byte bVar1;
  uint uVar2;
  uint uVar3;
  byte bVar4;
  bool bVar5;
  
  uVar2 = 1;
  bVar1 = 1;
  do {
    bVar4 = bVar1 * '\x02' ^ bVar1;
    bVar5 = (char)bVar1 < '\0';
    bVar1 = bVar4;
    if (bVar5) {
      bVar1 = bVar4 ^ 0x1b;
    }
    uVar2 = uVar2 * 2 ^ uVar2;
    uVar2 = uVar2 * 4 ^ uVar2;
    uVar3 = uVar2 << 4 ^ uVar2;
    uVar2 = uVar3 ^ 9;
    if (-1 < (char)uVar3) {
      uVar2 = uVar3;
    }
    bVar4 = (byte)uVar2;
    param_1[bVar1] =
         (bVar4 << 4 | bVar4 >> 4) ^ (bVar4 << 1 | (char)bVar4 < '\0') ^ bVar4 ^
         (bVar4 << 3 | bVar4 >> 5) ^ (bVar4 << 2 | bVar4 >> 6) ^ 99;
  } while (bVar1 != 1);
  *param_1 = 99;
  return;
}



void _DT_FINI(void)

{
  return;
}


