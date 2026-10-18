// Formato de este proyecto (ver .claude/rules/CONVENCIONES_GIT.md):
//   tipo[SCOPE]: descripción concreta
// NO es el formato estándar de Conventional Commits (que usa "tipo(scope):"
// con paréntesis) -- por eso hace falta un parserPreset custom en vez de
// extender @commitlint/config-conventional.
module.exports = {
  parserPreset: {
    parserOpts: {
      headerPattern: /^(\w+)\[([A-Z]+)\]: (.+)$/,
      headerCorrespondence: ['type', 'scope', 'subject'],
    },
  },
  rules: {
    'type-enum': [
      2,
      'always',
      ['feat', 'fix', 'refactor', 'docs', 'test', 'chore', 'style', 'perf', 'build', 'ci'],
    ],
    'scope-enum': [2, 'always', ['BACK', 'FRONT', 'MOBILE', 'INFRA', 'DOCS', 'REPO']],
    'type-empty': [2, 'never'],
    'type-case': [2, 'always', 'lower-case'],
    'scope-empty': [2, 'never'],
    'subject-empty': [2, 'never'],
    'header-max-length': [2, 'always', 100],
  },
}
