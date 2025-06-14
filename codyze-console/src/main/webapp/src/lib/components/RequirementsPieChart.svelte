<script lang="ts">
  import { onMount } from 'svelte';
  import { Chart, registerables } from 'chart.js';

  interface Props {
    fulfilled: number;
    violated: number;
    rejected: number;
    undecided: number;
  }

  let { fulfilled, violated, rejected, undecided }: Props = $props();

  let canvas: HTMLCanvasElement;
  let chart: Chart | null = null;

  // Register Chart.js components
  Chart.register(...registerables);

  onMount(() => {
    const total = fulfilled + violated + rejected + undecided;
    
    if (total === 0) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    chart = new Chart(ctx, {
      type: 'pie',
      data: {
        labels: ['Fulfilled', 'Violated', 'Rejected', 'Undecided'],
        datasets: [{
          data: [fulfilled, violated, rejected, undecided],
          backgroundColor: [
            'rgb(34, 197, 94)', // green-500 - matches fulfilled cards
            'rgb(239, 68, 68)', // red-500 - matches violated cards  
            'rgb(249, 115, 22)', // orange-500 - matches rejected cards
            'rgb(234, 179, 8)', // yellow-500 - matches undecided cards
          ],
          borderColor: [
            'rgb(255, 255, 255)', // white borders for clean look
            'rgb(255, 255, 255)',
            'rgb(255, 255, 255)', 
            'rgb(255, 255, 255)',
          ],
          borderWidth: 2
        }]
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
                size: 13,
                family: 'Inter, ui-sans-serif, system-ui, sans-serif'
              },
              color: 'rgb(55, 65, 81)' // gray-700 for text
            }
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
              label: function(context) {
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

    return () => {
      if (chart) {
        chart.destroy();
      }
    };
  });

  // Update chart when data changes
  $effect(() => {
    if (chart) {
      const total = fulfilled + violated + rejected + undecided;
      chart.data.datasets[0].data = [fulfilled, violated, rejected, undecided];
      chart.update();
    }
  });
</script>

<div class="relative h-80 w-full">
  <canvas bind:this={canvas} class="h-full w-full"></canvas>
</div>
