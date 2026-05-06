
void EVP_CIPHER_CTX_init(EVP_CIPHER_CTX *a)

{
  ulong uVar1;
  int iVar2;
  uint uVar3;
  EVP_CIPHER_CTX *pEVar5;
  EVP_CIPHER_CTX *pEVar6;
  bool bVar7;
  ulong uVar4;
  
  bVar7 = ((ulong)a & 1) != 0;
  uVar4 = 0xa8;
  iVar2 = 0xa8;
  if (bVar7) {
    *(undefined1 *)&a->cipher = 0;
    a = (EVP_CIPHER_CTX *)((long)&a->cipher + 1);
    uVar4 = 0xa7;
    iVar2 = 0xa7;
    uVar1 = (ulong)a & 2;
  }
  else {
    uVar1 = (ulong)a & 2;
  }
  if (uVar1 == 0) {
    uVar3 = (uint)uVar4;
    pEVar5 = a;
  }
  else {
    pEVar5 = (EVP_CIPHER_CTX *)((long)&a->cipher + 2);
    uVar3 = iVar2 - 2;
    uVar4 = (ulong)uVar3;
    *(undefined2 *)&a->cipher = 0;
  }
  if (((ulong)pEVar5 & 4) != 0) {
    *(undefined4 *)&pEVar5->cipher = 0;
    uVar4 = (ulong)(uVar3 - 4);
    pEVar5 = (EVP_CIPHER_CTX *)((long)&pEVar5->cipher + 4);
  }
  for (uVar1 = uVar4 >> 3; uVar1 != 0; uVar1 = uVar1 - 1) {
    pEVar5->cipher = (EVP_CIPHER *)0x0;
    pEVar5 = (EVP_CIPHER_CTX *)&pEVar5->engine;
  }
  if ((uVar4 & 4) != 0) {
    *(undefined4 *)&pEVar5->cipher = 0;
    pEVar5 = (EVP_CIPHER_CTX *)((long)&pEVar5->cipher + 4);
  }
  pEVar6 = pEVar5;
  if ((uVar4 & 2) != 0) {
    pEVar6 = (EVP_CIPHER_CTX *)((long)&pEVar5->cipher + 2);
    *(undefined2 *)&pEVar5->cipher = 0;
  }
  if (bVar7) {
    *(undefined1 *)&pEVar6->cipher = 0;
  }
  return;
}


