<?php

function classify(int $n): string
{
    if ($n < 0) {
        return "negative";
    } elseif ($n === 0) {
        return "zero";
    } else {
        return "positive";
    }
}

function sumUntil(int $limit): int
{
    $total = 0;
    $i = 1;
    while ($i <= $limit) {
        $total += $i;
        $i++;
    }
    return $total;
}

function sumArray(array $arr): int
{
    $sum = 0;
    foreach ($arr as $v) {
        $sum += $v;
    }
    return $sum;
}

function riskyOperation(): string
{
    try {
        if (rand() < 0) {
            throw new \RuntimeException("unexpected");
        }
        return "ok";
    } catch (\RuntimeException $e) {
        return "error: " . $e->getMessage();
    } finally {
        echo "done";
    }
}
