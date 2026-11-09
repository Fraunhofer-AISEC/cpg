/* eslint-disable svelte/prefer-svelte-reactivity --
   The Maps and Sets here are temporary values of derived computations or replaced as a whole, they
   are never mutated after being published, so they do not need to be reactive. */
import {
  checkDiagnostics,
  extractCitations,
  extractNodeIds,
  resolveNodes
} from '$lib/agentEvidence';
import type { ChatMessage, EvidenceDiagnosticJSON, NodeJSON } from '$lib/types';

/** One message of the conversation, placed in the timeline. */
export interface TimelineEntry {
  message: ChatMessage;
  /** The index of the message in the conversation */
  index: number;
  /** The number of the step, for tool calls */
  step?: number;
}

/** A question with everything the agent did to answer it. */
export interface TimelineBlock {
  question: ChatMessage | null;
  entries: TimelineEntry[];
}

/** The nodes that one tool call (a step) returned. */
export interface StepEvidence {
  step: number;
  ids: string[];
}

/** The evidence of a block: what each step returned and what is only cited in the answer. */
export interface BlockEvidence {
  steps: StepEvidence[];
  /** The IDs that are only cited in the answer, not returned by a step */
  claimed: string[];
}

/** A tool call of the active block that has evidence in the code. */
export interface ThreadStep {
  step: number;
  nodes: NodeJSON[];
  label: string;
  title: string;
}

/** What the evidence is derived from. It is read reactively, so it can be a getter of props. */
export interface ConversationSource {
  messages: ChatMessage[];
  streamingContent: string;
  isLoading: boolean;
  /** Whether the layer of the agent is shown in the code */
  agentVisible: boolean;
}

/** The number of diagnostics shown for a block before the rest is expanded. */
export const DIAGNOSTICS_PREVIEW = 3;

/**
 * The evidence trail of a conversation with the agent: the conversation as a timeline of questions
 * and tool calls (steps), the nodes the steps returned or the answers cite, and the places where
 * the analysis is uncertain that they rely on.
 *
 * It has to be created while a component is initialized, because it keeps its resolved nodes and
 * diagnostics up to date with effects.
 */
export class ConversationEvidence {
  private readonly source: () => ConversationSource;

  /** The nodes cited in the answers, resolved to their locations to label and open them */
  citedRefs = $state.raw<Map<string, NodeJSON>>(new Map());

  /** The nodes of the evidence, resolved to their locations. IDs that are no nodes are missing */
  evidenceRefs = $state.raw<Map<string, NodeJSON>>(new Map());

  /**
   * The places where the analysis is incomplete, uncertain or ends that the evidence of each block
   * relies on. The block the agent is still working on is checked once it is done.
   */
  blockDiagnostics = $state.raw<EvidenceDiagnosticJSON[][]>([]);

  /** The block the user chose to show in the code, or null for the latest one with evidence */
  chosenBlock = $state<number | null>(null);

  /** The blocks whose diagnostics are shown in full, otherwise only the first few are shown */
  diagnosticsExpanded = $state<Record<number, boolean>>({});

  /** The conversation as a timeline: one block per question, with the tool calls as steps */
  timeline = $derived.by(() => {
    const blocks: TimelineBlock[] = [];
    this.source().messages.forEach((message, index) => {
      if (message.role === 'user') {
        blocks.push({ question: message, entries: [] });
        return;
      }
      if (blocks.length === 0) blocks.push({ question: null, entries: [] });
      const block = blocks[blocks.length - 1];
      const step =
        message.contentType === 'tool-result'
          ? block.entries.filter((e) => e.step).length + 1
          : undefined;
      block.entries.push({ message, index, step });
    });
    return blocks;
  });

  /** The evidence of each block */
  blockEvidence: BlockEvidence[] = $derived(
    this.timeline.map((block) => {
      const steps = block.entries.flatMap((e) =>
        e.step ? [{ step: e.step, ids: extractNodeIds(e.message.toolResult?.content) }] : []
      );
      const returned = new Set(steps.flatMap((s) => s.ids));
      const cited = block.entries.flatMap((e) =>
        e.message.contentType === 'text' ? extractCitations(e.message.content) : []
      );
      return { steps, claimed: [...new Set(cited)].filter((id) => !returned.has(id)) };
    })
  );

  /** The block whose evidence is shown in the code: the chosen one, else the latest with evidence */
  activeBlock = $derived(
    this.chosenBlock !== null && this.chosenBlock < this.timeline.length
      ? this.chosenBlock
      : this.blockEvidence.findLastIndex(
          (e) => e.steps.some((s) => s.ids.length) || e.claimed.length
        )
  );

  /**
   * The tool calls of the active block that have evidence in the code, to step through them like in
   * a debugger. Nodes only cited in the answer are no step: they are marked in the code and named
   * in the diagnostics under the answer.
   */
  threadSteps: ThreadStep[] = $derived.by(() => {
    const evidence = this.blockEvidence[this.activeBlock];
    const block = this.timeline[this.activeBlock];
    if (!evidence || !block || !this.source().agentVisible) return [];
    return evidence.steps.flatMap(({ step, ids }) => {
      const nodes = this.stepNodes(ids);
      const entry = block.entries.find((e) => e.step === step);
      const tool = entry?.message.toolResult?.toolName ?? 'tool';
      return nodes.length
        ? [{ step, nodes, label: tool, title: `Step ${step}: ${tool}, ${nodes.length} nodes` }]
        : [];
    });
  });

  private readonly citedKey: string;

  private evidenceKey = $derived(
    [
      ...new Set(
        this.blockEvidence.flatMap((e) => [...e.steps.flatMap((s) => s.ids), ...e.claimed])
      )
    ]
      .sort()
      .join(',')
  );

  constructor(source: () => ConversationSource) {
    this.source = source;
    this.citedKey = $derived(
      [
        ...new Set([
          ...this.source().messages.flatMap((m) =>
            m.role === 'assistant' && m.contentType !== 'tool-result'
              ? extractCitations(m.content)
              : []
          ),
          ...extractCitations(this.source().streamingContent)
        ])
      ].join(',')
    );

    $effect(() => {
      const key = this.citedKey;
      if (!key) return;
      resolveNodes(key.split(',')).then((refs) => {
        if (key === this.citedKey) this.citedRefs = refs;
      });
    });

    $effect(() => {
      const key = this.evidenceKey;
      if (!key) return;
      resolveNodes(key.split(',')).then((refs) => {
        if (key === this.evidenceKey) this.evidenceRefs = refs;
      });
    });

    $effect(() => {
      const evidence = this.blockEvidence;
      const pending = this.source().isLoading ? evidence.length - 1 : -1;
      Promise.all(
        evidence.map((e, b) =>
          b === pending ? [] : checkDiagnostics([...e.steps.flatMap((s) => s.ids), ...e.claimed])
        )
      ).then((diagnostics) => {
        if (evidence === this.blockEvidence) this.blockDiagnostics = diagnostics;
      });
    });
  }

  /** The resolved nodes of the given IDs, without those that are no nodes. */
  stepNodes(ids: string[]): NodeJSON[] {
    return ids.flatMap((id) => this.evidenceRefs.get(id) ?? []);
  }

  /** Who relies on a diagnostic of a block, e.g. "Step 1", "Steps 1, 3" or "The answer". */
  diagnosticSubject(
    diagnostic: EvidenceDiagnosticJSON,
    block: number
  ): { text: string; plural: boolean } {
    const steps = (this.blockEvidence[block]?.steps ?? [])
      .filter((s) => s.ids.some((id) => diagnostic.evidence.includes(id)))
      .map((s) => s.step);
    if (steps.length === 0) return { text: 'The answer', plural: false };
    return {
      text: `${steps.length === 1 ? 'Step' : 'Steps'} ${steps.join(', ')}`,
      plural: steps.length > 1
    };
  }
}
