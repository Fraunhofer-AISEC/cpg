<?php

namespace App\Service;

class Counter
{
    private int $value;

    public function __construct(int $initial = 0)
    {
        $this->value = $initial;
    }

    public function increment(int $by = 1): void
    {
        $this->value += $by;
    }

    public function get(): int
    {
        return $this->value;
    }
}

CLASS Gauge
{
    PRIVATE int $value;

    PUBLIC FUNCTION __construct(int $initial = 1)
    {
        $this->value = $initial;
    }

    PUBLIC FUNCTION read(): int
    {
        RETURN $this->value;
    }
}
