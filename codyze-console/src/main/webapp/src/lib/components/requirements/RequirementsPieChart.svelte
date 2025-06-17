<script lang="ts">
  import { onMount } from 'svelte';
  import { Chart, registerables } from 'chart.js';

  interface Props {
    fulfilled: number;
    notFulfilled: number;
    rejected: number;
    undecided: number;
    notYetEvaluated: number;
  }

  let { fulfilled, notFulfilled, rejected, undecided, notYetEvaluated }: Props = $props();

  let canvas: HTMLCanvasElement;
  let chart: Chart | null = null;

  // Register Chart.js components
  Chart.register(...registerables);

  // Recreate chart when data structure changes to ensure colors are correct
  $effect(() => {
    const total = fulfilled + notFulfilled + rejected + undecided + notYetEvaluated;

    // Destroy existing chart
    if (chart) {
      chart.destroy();
      chart = null;
    }

    if (total === 0 || !canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    chart = new Chart(ctx, {
      type: 'pie',
      data: {
        labels: ['Fulfilled', 'Not Fulfilled', 'Rejected', 'Undecided', 'Not Yet Evaluated'],
        datasets: [
          {
            data: [fulfilled, notFulfilled, rejected, undecided, notYetEvaluated],
            backgroundColor: [
              'rgb(34, 197, 94)', // green-500 - matches fulfilled cards
              'rgb(239, 68, 68)', // red-500 - matches not fulfilled cards
              'rgb(249, 115, 22)', // orange-500 - matches rejected cards
              'rgb(234, 179, 8)', // yellow-500 - matches undecided cards
              'rgb(107, 114, 128)' // gray-500 - matches not yet evaluated cards
            ],
            borderColor: [
              'rgb(255, 255, 255)', // white borders for clean look
              'rgb(255, 255, 255)',
              'rgb(255, 255, 255)',
              'rgb(255, 255, 255)',
              'rgb(255, 255, 255)'
            ],
            borderWidth: 2
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              usePointStyle: true,
              padding: 20,
              font: {
                size: 12
              }
            }
          },
          tooltip: {
            callbacks: {
              label: function (context: any) {
                const label = context.label || '';
                const value = context.parsed;
                const percentage = ((value / total) * 100).toFixed(1);
                return `${label}: ${value} (${percentage}%)`;
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
