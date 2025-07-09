#include <winbase.h>
#include <libloaderapi.h>

int WinMain(
    HINSTANCE hInstance,
    HINSTANCE hPrevInstance,
    LPSTR lpCmdLine,
    int nCmdShow
) {
    HMODULE lib = LoadLibraryA("winexample.dll");
    return 0;
}
