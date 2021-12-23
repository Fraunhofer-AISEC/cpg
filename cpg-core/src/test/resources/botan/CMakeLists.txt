cmake_minimum_required(VERSION 3.12)
project(botan)

set(CMAKE_CXX_STANDARD 17)

include_directories(/usr/include/botan-2)

link_libraries(botan-2)

add_executable(symm_block_cipher symm_block_cipher.cpp)