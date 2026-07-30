// Build the plugin using https://github.com/jenkins-infra/pipeline-library
buildPlugin(useContainerAgent: true, failFast: false, forkCount: '1C', configurations: [
  [platform: 'windows', jdk: 21],
  [platform: 'windows', jdk: 25],
  [platform: 'linux', jdk: 21],
  [platform: 'linux', jdk: 25],
])
