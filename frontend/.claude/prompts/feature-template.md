# Features descriptions

Create a new feature: {{feature_name}}

Detailed description: {{description}}

Technical requirements:
- Framework used: {{framework}}
- Component type: {{component_type}}
- Required tests: {{tests_required}}

Specific instructions: {{instructions}}

## Examples of using imported prompts

### 1. Basic Usage
Simply type `@feature-template` in the conversation with Claude Code to trigger this prompt.

### 2. Typical use cases
Simply type `@feature1` in the conversation with Claude Code to trigger this prompt.

#### Development of new features
```
@feature1 feature_name="Authentication" description="Login/logout system with JWT" framework="Nuxt.js" component_type="Composable + Context" tests_required="true" instructions="Use localStorage for the token"
```

#### Improvement of an existing feature  
```
@feature1 feature_name="Form Validation" description="Real-time error messages" framework="Nuxt.js" component_type="Custom Hook" tests_required="true" instructions="Use Nuxt.js component form"
```

#### code Refactoring
```
@feature1 feature_name="Optimization Header" description="Improve performance" framework="Nuxt.js" component_type="Functional Component" tests_required="false" instructions="Use Nuxt.js.memo and useMemo"
```

### 3. Advantages of prompts

- **Reusability**: Same prompt for different projects
- **Consistency**: Standardized approach for each feature
- **Efficiency**: Avoids rewriting the same instructions
- **Collaboration**: Teams use the same prompts

### 4. Parameter Syntax 

- **Variables**: `{{variable_name}}` in the prompt
- **Passage**: `@feature-template variable_name="value"`
- **Multiples**: `@feature-template param1="value1" param2="value2"`
- **Spaces**: Use quotes for values ​​with spaces

### 5. Best practices

- Define clear variables with `{{variable}}`
- Use descriptive parameter names
- Document expected parameters in comments
- Test with different parameter values
