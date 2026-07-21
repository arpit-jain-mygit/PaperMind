# LangChain vs LangGraph vs LangSmith

## Overview
These three tools from LangChain ecosystem serve different purposes in building and managing LLM-powered applications:
- **LangChain**: Framework for building LLM applications
- **LangGraph**: Orchestration layer for complex workflows
- **LangSmith**: Development and monitoring platform

## Reference Materials

### Videos
- **LangChain vs LangGraph vs LangSmith** by codebasics
  - https://www.youtube.com/watch?v=vJOGC8QJZJQ
  - Comprehensive comparison of these three tools and their use cases

## LangChain
**Purpose**: Core framework for building LLM applications

### Key Features
- Prompt templates and chaining
- LLM integrations
- Memory management
- Document loading and processing
- Agent capabilities

### Use Cases
- Simple to moderately complex LLM applications
- Rapid prototyping of AI features
- Integration with various LLM providers

---

## LangGraph
**Purpose**: Graph-based orchestration for complex, multi-step workflows

### Key Features
- State-based execution model
- Cyclical workflow support
- Tool calling and agent orchestration
- Streaming capabilities
- Human-in-the-loop interactions

### Use Cases
- Multi-turn agent workflows
- Agentic systems with loops and branches
- Complex reasoning flows
- Autonomous agents

---

## LangSmith
**Purpose**: Development, monitoring, and debugging platform

### Key Features
- Tracing and logging
- Dataset management
- A/B testing capabilities
- Prompt management
- Performance monitoring
- Cost tracking

### Use Cases
- Application debugging and optimization
- Production monitoring
- Evaluation and testing
- Team collaboration

---

## Quick Comparison

| Aspect | LangChain | LangGraph | LangSmith |
|--------|-----------|-----------|-----------|
| Type | Framework | Orchestration | Platform |
| Complexity | Low to Medium | Medium to High | N/A |
| Workflow Type | Linear chains | Graphs/Cycles | Monitoring |
| Agent Support | Basic | Advanced | Observability |
| Production Ready | Yes | Yes | Yes |

---

## When to Use What

- **LangChain**: Building straightforward LLM applications and prototypes
- **LangGraph**: Complex multi-agent systems, workflows with loops and conditional logic
- **LangSmith**: Debugging, monitoring, and optimizing any LLM application in production
