# Prompt Service

The **Prompt Service** is part of the [open4goods](https://github.com/open4good/open4goods) project. It provides capabilities to interact with generative AI services (OpenAI and Gemini) by loading prompt templates, evaluating them using Thymeleaf, and executing chat-based requests.

## Features

- Loads YAML prompt templates from a configurable folder.
- Supports multiple AI backends (OpenAI and Gemini).
- Evaluates dynamic variables in prompt templates.
- Returns both raw and parsed responses.

## Requirements

- Java 11 or higher
- Maven 3.6+
- Spring Boot

## Installation

Clone the repository and build using Maven:

```bash
git clone https://github.com/open4good/open4goods.git
cd open4goods/services/prompt
mvn clean install
```

## Usage

### Configuring the Service

Set the following properties (for example, in your `application.yml`):

```yaml
genAiConfig:
  promptsTemplatesFolder: "src/main/resources/prompts"
  cacheTemplates: true
  enabled: true
```

For provider credentials and batch options, see `docs/genai.md` at the repository root.

### Calling the Service

Below is an example of using the service in your application:

```java
// Inject GenAiService via Spring
@Autowired
private GenAiService genAiService;

public void executePrompt() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("username", "John Doe");

    try {
        PromptResponse<?> response = genAiService.prompt("impactscore-prompt", variables);
        System.out.println("Raw Response: " + response.getRaw());
    } catch (Exception e) {
        // Handle exceptions appropriately
        e.printStackTrace();
    }
}
```


### Prompt sample

Below is an example of prompt, the one used by Nudger to compute the Impact Score

```yaml


#####################################################################################
# Represents a prompt config (a chat model configuration and a chat model options),
# associated to system and user templatised prompts
#
# This template uses the following variables : 
# VERTICAL_NAME  : The vertical name (vConf.getI18n().get("fr").getVerticalHomeTitle()))
# AVAILABLE_CRITERIAS : The availlable criterias, (key : description\n) 
#
#####################################################################################
# The unique key used to identify this prompt config
key: "impactscore-prompt"

# The Gen ai service to use
aiService: "OPEN_AI"

# The system prompt
# No system prompt with o1
#systemPrompt: |
#  Tu es un agent expert en évaluation environnementale des produits électriques et electroniques.
#  Adopte une démarche d’expert en analyse du cycle de vie des produits. 
#  Nous allons travailler sur des [[${VERTICAL_NAME}]]
#  Ne fournir en réponse que du JSON, conformément à la structure indiquée, sans commentaire, ni texte supplémentaire

# The user prompt
userPrompt: |


    ## Ton rôle
    Tu es un agent expert en évaluation environnementale des produits électriques et electroniques.
    Adopte une démarche d’expert en analyse du cycle de vie des produits. 
    Nous allons travailler sur des [[${VERTICAL_NAME}]]
    Ne fournir en réponse que du JSON, conformément à la structure indiquée, sans commentaire, ni texte supplémentaire

    ### Création d'un eco-score pour les [[${VERTICAL_NAME}]] 

    Tu vas créer un score d'impact environnemental, qui prend en compte les impacts écologiques et sociétaux pour les [[${VERTICAL_NAME}]].
    
    Cet Impact Score est une composition de différents facteurs coefficientés. Les facteurs disponibles sont : 
    [[${AVAILABLE_CRITERIAS}]]
  
    ### Principe de fonctionnement de l'eco-score
       
    Les principes de fonctionnement que tu dois prendre en compte
       principe de relativisation des facteurs : Chacun des facteurs est représenté de manière relative, sous forme de classement. Le produit de la catégorie ayant le meilleur facteur pour l'environnement obtient 100/100, le produit ayant le moins bon score obtient 0/100' 
       principe de virtualisation des scores manquants : si un des facteurs est absent, nous appliquons pour ce facteur la valeur moyenne de ce facteur pour l'ensemble des produits. Cet indicateur DATA_QUALITY est permet donc d'avantager les produits pour lequel toute l'info est disponible, sans pour autant pénaliser outre mesure les produits pour lesquels l'information est absente.
       la somme des différents facteurs coefficientés doit être égale à 1
       
    Tu vas travailler à partir des facteurs disponibles, pour élaborer un score d'impact environnemental pertinent pour des [[${VERTICAL_NAME}]].
    
    ### Format de réponse attendu : JSON 
        
     Tout écart par rapport à la structure JSON fournie est inacceptable.
     Aucune information supplémentaire ne doit être fournie en dehors du JSON (pas de phrases avant ou après).
     Conserve l’ordre des clés et leur orthographe. Les clés doivent correspondre exactement à celles indiquées. Si un champ est nul ou non applicable, omets-le de la réponse.
     Fournis une réponse JSON respectant strictement la structure suivante :

     {
        criteriasPonderation : {
          "FACTEUR_1" : PONDERATION_1,
          "FACTEUR_2" : PONDERATION_2,
          ...                        
        },
        texts: {
            fr: {
               "purpose": "Décris la démarche et la méthodologie",
               "availlableDatas": "Analyse de façon générale les données disponibles et leur pertinence pour la réalisation de cet eco-score",
               "criticalReview": "Revue critique et retour constructif sur la démarche et la méthodologie, en évaluant les facteurs absents ou inutiles",
               "criteriasAnalysis": {
                    "FACTEUR_1" : "Détail et analyse de l'importance du FACTEUR_1 dans l'analyse environnemental des [[${VERTICAL_NAME}]]. Explique et justifie la pondération retenue pour le FACTEUR_1",
                    "FACTEUR_2" : "Détail et analyse de l'importance du FACTEUR_2 dans l'analyse environnemental des [[${VERTICAL_NAME}]]. Explique et justifie la pondération retenue pour le FACTEUR_2",
                    ...                                    
               }
            }    
         }
     }
# The options (temperature, top k...) given to the chat model
options:
  # ID of the model to use
  #model: "gpt-4o"
  model: "o1-preview"  
  #model: "llama-3.1-sonar-small-128k-online"
  
  # Sampling temperature to use, between 0 and 1
  # Only default temperature (1) with 01-preview
  temperature: 1

          
  # An object specifying the format that the model must output
 # response-format:
 #   type: JSON_OBJECT

      # Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far,
  # decreasing the model's likelihood to repeat the same line verbatim.
  #frequency_penalty: 0.0

  # Modify the likelihood of specified tokens appearing in the completion. Accepts a JSON object
  # that maps tokens (specified by their token ID in the tokenizer) to an associated bias value from -100 to 100.
  #logit_bias: {}

  # Whether to return log probabilities of the output tokens or not
  #logprobs: false

  # An integer between 0 and 5 specifying the number of most likely tokens to return at each token position
  #top_logprobs: 0

  # The maximum number of tokens to generate in the chat completion
  #max_tokens: 100

  # An upper bound for the number of tokens that can be generated for a completion
  #max_completion_tokens: 0

  # How many chat completion choices to generate for each input message
  #n: 1

  # Output types that you would like the model to generate for this request
  #modalities: []

  # Audio parameters for the audio generation
  #audio: {}

  # Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the text so far
  #presence_penalty: 0.0

  # Options for streaming response
  #stream_options: {}

  # A seed value to enable deterministic output
  #seed: 0

  # Up to 4 sequences where the API will stop generating further tokens
  #stop: []


  # Nucleus sampling probability mass, between 0 and 1
  #top_p: 0.0

  # A list of tools the model may call
  #tools: []

  # Controls which (if any) function is called by the model
  #tool_choice: ""

  # A unique identifier representing your end-user
  #user: ""

  # Whether to enable parallel function calling
  #parallel_tool_calls: false


```


### Testing

A minimal test configuration is provided in the test source folder. To run the tests:

```bash
mvn test
```

## Contributing

Contributions are welcome! Please submit a pull request or open an issue for any improvements or bugs.

## License

This project is licensed under the MIT License.
