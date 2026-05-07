<?php

function greet(string $name): string
{
    return "Hello " . $name;
}

function sum(int $a, int $b = 0): int
{
    return $a + $b;
}

function joinAll(string ...$parts): string
{
    return implode(",", $parts);
}
