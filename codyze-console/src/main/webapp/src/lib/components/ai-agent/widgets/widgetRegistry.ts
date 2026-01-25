import type { Component } from 'svelte';

export type ToolResultData = {
  toolName?: string;
  content: any;
  isError?: boolean;
  isPending?: boolean;
};

export type WidgetProps = {
  data: ToolResultData;
  onItemClick?: (item: any) => void;
};

export type WidgetComponent = Component<WidgetProps>;

type WidgetMatcher = (data: ToolResultData) => boolean;

interface WidgetRegistration {
  component: WidgetComponent;
  matcher: WidgetMatcher;
}

class WidgetRegistry {
  private registrations: WidgetRegistration[] = [];

  register(component: WidgetComponent, matcher: WidgetMatcher) {
    this.registrations.push({ component, matcher });
  }

  /**
   * Find the appropriate widget component for the given data.
   * Returns null if no matching widget is found.
   */
  getWidget(data: ToolResultData): WidgetComponent | null {
    for (let i = 0; i < this.registrations.length; i++) {
      const registration = this.registrations[i];
      const matches = registration.matcher(data);
      if (matches) {
        return registration.component;
      }
    }
    return null;
  }
}

export const widgetRegistry = new WidgetRegistry();