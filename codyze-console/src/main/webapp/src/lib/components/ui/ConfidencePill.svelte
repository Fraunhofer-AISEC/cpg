<script lang="ts">
  export type ConfidenceType = 'UndecidedResult' | 'RejectedResult' | 'AcceptedResult';
  
  interface Props {
    confidence: ConfidenceType;
    size?: 'sm' | 'md';
  }

  let { confidence, size = 'sm' }: Props = $props();

  const confidenceConfig = {
    'UndecidedResult': {
      label: 'UndecidedResult',
      bgColor: 'bg-yellow-100',
      textColor: 'text-yellow-800',
      borderColor: 'border-yellow-200'
    },
    'RejectedResult': {
      label: 'RejectedResult',
      bgColor: 'bg-red-100',
      textColor: 'text-red-800',
      borderColor: 'border-red-200'
    },
    'AcceptedResult': {
      label: 'AcceptedResult',
      bgColor: 'bg-green-100',
      textColor: 'text-green-800',
      borderColor: 'border-green-200'
    }
  };

  const config = $derived(confidenceConfig[confidence]);
  const sizeClasses = $derived(size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-3 py-1 text-sm');
</script>

<span class="inline-flex items-center rounded-full border font-medium {config.bgColor} {config.textColor} {config.borderColor} {sizeClasses}">
  {config.label}
</span>
