<script lang="ts">
  import { onMount } from 'svelte';
  import { Chart, registerables } from 'chart.js';

  interface Props {
    fulfilled: number;
    violated: number;
    rejected: number;
    undecided: number;
    notYetEvaluated: number;
  }

  let { fulfilled, violated, rejected, undecided, notYetEvaluated }: Props = $props();

  let canvas: HTMLCanvasElement;
  let chart: Chart | null = null;

  // Register Chart.js components
  Chart.register(...registerables);

  // Recreate chart when data changes to ensure colors are correct
  $effect(() => {
    const total = fulfilled + violated + rejected + undecided + notYetEvaluated;

    // Destroy existing chart
    if (chart) {
      chart.destroy();
      chart = null;
    }

    if (total === 0 || !canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    chart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: ['Fulfilled', 'Violated', 'Rejected', 'Undecided', 'Not Yet Evaluated'],
        datasets: [
          {
            label: 'Requirements',
            data: [fulfilled, violated, rejected, undecided, notYetEvaluated],
            backgroundColor: [
              'rgb(34, 197, 94)', // green-500 - matches fulfilled cards
              'rgb(239, 68, 68)', // red-500 - matches violated cards
              'rgb(249, 115, 22)', // orange-500 - matches rejected cards
              'rgb(234, 179, 8)', // yellow-500 - matches undecided cards
              'rgb(107, 114, 128)' // gray-500 - matches not yet evaluated cards
            ],
            borderColor: [
              'rgb(34, 197, 94)', // same as background for clean look
              'rgb(239, 68, 68)',
              'rgb(249, 115, 22)',
              'rgb(234, 179, 8)',
              'rgb(107, 114, 128)'
            ],
            borderWidth: 0,
            borderRadius: 6,
            borderSkipped: false
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              stepSize: 1,
              font: {
                family: 'Inter, ui-sans-serif, system-ui, sans-serif',
                size: 12
              },
              color: 'rgb(107, 114, 128)'
            },
            grid: {
              color: 'rgb(243, 244, 246)'
            }
          },
          x: {
            ticks: {
              font: {
                family: 'Inter, ui-sans-serif, system-ui, sans-serif',
                size: 12
              },
              color: 'rgb(107, 114, 128)'
            },
            grid: {
              display: false
            }
          }
        },
        plugins: {
          legend: {
            display: false
          },
          tooltip: {
            backgroundColor: 'rgb(255, 255, 255)',
            titleColor: 'rgb(17, 24, 39)',
            bodyColor: 'rgb(55, 65, 81)',
            borderColor: 'rgb(229, 231, 235)',
            borderWidth: 1,
            cornerRadius: 8,
            titleFont: {
              family: 'Inter, ui-sans-serif, system-ui, sans-serif',
              size: 14,
              weight: 600
            },
            bodyFont: {
              family: 'Inter, ui-sans-serif, system-ui, sans-serif',
              size: 13
            },
            callbacks: {
              label: function (context: any) {
                const value = context.parsed.y;
                const percentage = ((value / total) * 100).toFixed(1);
                return `Count: ${value} (${percentage}%)`;
              }
            }
          }
        }
      }
    });
  });

  onMount(() => {
    return () => {
      if (chart) {
        chart.destroy();
      }
    };
  });
</script>

<div class="relative h-80 w-full">
  <canvas bind:this={canvas} class="h-full w-full"></canvas>
</div>
