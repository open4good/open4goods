import { parse } from '@vue/compiler-sfc'
import {
  NodeTypes,
  baseParse,
  ElementTypes,
  type DirectiveNode,
  type ElementNode,
  type ForNode,
  type IfBranchNode,
  type IfNode,
  type InterpolationNode,
  type RootNode,
  type SimpleExpressionNode,
  type TemplateChildNode,
  type TextCallNode,
} from '@vue/compiler-dom'
import MagicString from 'magic-string'
import type { Plugin } from 'vite'

const DATA_KEY_ATTRIBUTE = 'data-i18n-key'

function extractStaticI18nKey(expression: string): string | null {
  const normalized = expression.trim()
  const standardMatch = normalized.match(/^\$?t\s*\(\s*(['"])([^'"\s]+)\1/)
  if (standardMatch) {
    return standardMatch[2]
  }

  const templateLiteralMatch = normalized.match(/^\$?t\s*\(\s*`([^`]+)`/)
  if (templateLiteralMatch) {
    return templateLiteralMatch[1]
  }

  return null
}

function recordAnnotation(map: Map<ElementNode, Set<string>>, element: ElementNode, key: string) {
  if (!map.has(element)) {
    map.set(element, new Set([key]))
    return
  }

  map.get(element)!.add(key)
}

function traverseTemplate(root: RootNode): Map<ElementNode, Set<string>> {
  const annotations = new Map<ElementNode, Set<string>>()

  function traverse(node: TemplateChildNode | RootNode, parentElement: ElementNode | null) {
    switch (node.type) {
      case NodeTypes.ROOT:
        (node as RootNode).children.forEach((child) => traverse(child, parentElement))
        break
      case NodeTypes.ELEMENT: {
        const current = node as ElementNode
        if (current.tagType !== ElementTypes.ELEMENT) {
          current.children.forEach((child) => traverse(child, parentElement))
          break
        }

        current.props.forEach((prop) => {
          if (prop.type === NodeTypes.DIRECTIVE) {
            const directive = prop as DirectiveNode & { exp: SimpleExpressionNode | undefined }
            if (directive.exp && (directive.name === 'text' || directive.name === 'html') && directive.exp.content) {
              const key = extractStaticI18nKey(directive.exp.content)
              if (key) {
                recordAnnotation(annotations, current, key)
              }
            }
          }
        })

        current.children.forEach((child) => traverse(child, current))
        break
      }
      case NodeTypes.INTERPOLATION: {
        const interpolation = node as InterpolationNode
        if (!parentElement || interpolation.content.type !== NodeTypes.SIMPLE_EXPRESSION) {
          break
        }

        const key = extractStaticI18nKey(interpolation.content.content)
        if (key) {
          recordAnnotation(annotations, parentElement, key)
        }
        break
      }
      case NodeTypes.IF:
        (node as IfNode).branches.forEach((branch: IfBranchNode) => {
          branch.children.forEach((child) => traverse(child, parentElement))
        })
        break
      case NodeTypes.FOR:
        (node as ForNode).children.forEach((child) => traverse(child, parentElement))
        break
      case NodeTypes.TEXT_CALL:
        traverse((node as TextCallNode).content, parentElement)
        break
      default:
        break
    }
  }

  traverse(root, null)
  return annotations
}

function escapeAttributeValue(value: string): string {
  return value.replace(/&/g, '&amp;').replace(/"/g, '&quot;')
}

export function createI18nInContextVitePlugin(): Plugin {
  return {
    name: 'i18n-inctx-key-injector',
    enforce: 'pre',
    transform(code, id) {
      if (!id.endsWith('.vue')) {
        return null
      }

      const { descriptor } = parse(code, { filename: id })
      const templateBlock = descriptor.template

      if (!templateBlock || (templateBlock.lang && templateBlock.lang !== 'html')) {
        return null
      }

      const templateContent = templateBlock.content
      if (!templateContent.trim()) {
        return null
      }

      const ast = baseParse(templateContent, { comments: true })
      const annotations = traverseTemplate(ast)

      if (annotations.size === 0) {
        return null
      }

      const magicString = new MagicString(code)
      const templateOffset = templateBlock.loc.start.offset

      for (const [element, keys] of annotations.entries()) {
        if (keys.size !== 1) {
          continue
        }

        if (element.props.some((prop) => prop.type === NodeTypes.ATTRIBUTE && prop.name === DATA_KEY_ATTRIBUTE)) {
          continue
        }

        const [key] = Array.from(keys)
        const elementSource = templateContent.slice(element.loc.start.offset, element.loc.start.offset + element.loc.source.length)
        const tagEndIndex = elementSource.indexOf('>')

        if (tagEndIndex === -1) {
          continue
        }

        const absoluteIndex = templateOffset + element.loc.start.offset + tagEndIndex
        magicString.appendLeft(absoluteIndex, ` ${DATA_KEY_ATTRIBUTE}="${escapeAttributeValue(key)}"`)
      }

      return {
        code: magicString.toString(),
        map: magicString.generateMap({ hires: true }),
      }
    },
  }
}
