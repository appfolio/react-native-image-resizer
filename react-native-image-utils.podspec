require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name     = package['name']
  s.version  = package['version']
  s.authors  = package['author']
  s.license  = package['license']
  s.homepage = package['homepage']
  s.summary  = package['description']

  s.platform = :ios, "8.0"

  s.source   = { :git => "https://github.com/appfolio/react-native-image-utils.git", :tag => "v#{s.version}" }
  s.source_files   = "ios/**/*.{h,m}"
  s.preserve_paths = 'README.md', 'LICENSE', 'package.json', 'index.js'

  s.dependency 'React'
end
