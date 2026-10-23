# Skills

Use the series skills for the two distinct documentation workflows below. They complement [`AGENTS.md`](AGENTS.md) and [`plan.md`](plan.md); they do not authorize implementation or release actions.

| Skill | Use when | Output |
| --- | --- | --- |
| [Technical writing](.agents/skills/technical-writing/SKILL.md) | Drafting or revising a chapter article for an approved increment. | Source-aligned article draft with a concrete reader path, observable evidence, limitations, and next-step handoff. |
| [Article-to-code verification](.agents/skills/article-to-code-verification/SKILL.md) | Reviewing a chapter against a frozen candidate snapshot, especially before acceptance or PR. | Claim-to-evidence record with checks, observed results, gaps, and blockers. |

Use technical writing during chapter development. Once the candidate is frozen, use article-to-code verification as an independent acceptance pass. A change to source, article, dependencies, configuration, prompts, policies, schemas, or the integration base invalidates affected evidence and requires rechecking it.

## Reference Sources

- [Quarkus LangChain4j documentation](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html) is useful for current concepts and links into extension references. The `dev` path moves over time; it is not a version pin or compatibility guarantee.
- [Quarkus LangChain4j workshop](https://quarkus.io/quarkus-workshop-langchain4j/) demonstrates incremental teaching and observable exercises. Follow its current source-repository link for implementation examples; the workshop's architecture, APIs, versions, tests, and deployment choices are not proof of parity with this series.
- [Workshop repository agent guide](https://github.com/quarkusio/quarkus-langchain4j-workshop/blob/main/AGENTS.md), [workshop documentation style skill](https://github.com/quarkusio/quarkus-langchain4j-workshop/blob/main/.agents/skills/workshop-docs-style/SKILL.md), and its [Section 3 guide](https://github.com/quarkusio/quarkus-langchain4j-workshop/blob/main/section-3/README.md) inform style and snapshot/testing practices. Treat `main` as a moving reference, not a pinned source snapshot.

Before any implementation or capability claim, consult release-specific primary documentation and artifacts, pin versions, and produce compatibility evidence for the exact stack in use. The references above are inputs to these skills, not copied material; write original prose and examples and cite the relevant source beside the claim.
