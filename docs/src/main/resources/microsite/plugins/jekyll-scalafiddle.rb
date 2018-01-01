module Jekyll
  module ScalaFiddle
    class OptionsParser
      OPTIONS_SYNTAX = %r!([^\s]+)\s*=\s*['"]+([^'"]+)['"]+!
      ALLOWED_FLAGS = %w(
        autorun
      ).freeze
      ALLOWED_ATTRIBUTES = %w(
        template
        prefix
        dependency
        scalaversion
        selector
        minheight
        layout
        theme
      ).freeze

      class << self
        def parse(raw_options)
          options = {
              :attributes => {},
              :filters => {},
              :flags => {}
          }
          raw_options.scan(OPTIONS_SYNTAX).each do |key, value|
            if ALLOWED_ATTRIBUTES.include?(key)
              options[:attributes][key.to_sym] = value
            else
              options[:filters][key] = value
            end
          end
          ALLOWED_FLAGS.each do |key|
            options[:flags][key.to_sym] = true if raw_options.include?(key)
          end
          options
        end
      end
    end

    class ScalaFiddleTag < Liquid::Block

      def initialize(tag, args, _)
        @args = OptionsParser.parse(args)
        super
      end

      def render(context)
        site = context.registers[:site]
        converter = site.find_converter_instance(::Jekyll::Converters::Markdown)
        content = converter.convert(super(context))
        config = site.config.fetch("scalafiddle", {})
        result = <<HTML
<div #{render_attributes(config)}>#{content}</div>
HTML
        result
      end

      private
      def render_attributes(config)
        attributes = {"data-scalafiddle" => ""}

        if @args[:attributes][:template]
          attributes["data-template"] = "'#{@args[:attributes][:template]}'"
        end
        # apply default attributes from config
        if config.key?("dependency")
          attributes["data-dependency"] = "'#{config["dependency"]}'"
        end
        if config.key?("scalaversion")
          attributes["data-scalaversion"] = "'#{config["scalaversion"]}'"
        end
        if config.key?("selector")
          attributes["data-selector"] = "'#{config["selector"]}'"
        end
        if config.key?("theme")
          attributes["data-theme"] = "'#{config["theme"]}'"
        end
        # apply tag attributes
        if @args[:attributes][:dependency]
          attributes["data-dependency"] = "'#{@args[:attributes][:dependency]}'"
        end
        if @args[:attributes][:scalaversion]
          attributes["data-scalaversion"] = "'#{@args[:attributes][:scalaversion]}'"
        end
        if @args[:attributes][:selector]
          attributes["data-selector"] = "'#{@args[:attributes][:selector]}'"
        end
        if @args[:attributes][:prefix]
          attributes["data-prefix"] = "'#{@args[:attributes][:prefix]}'"
        end
        if @args[:attributes][:minheight]
          attributes["data-minheight"] = "'#{@args[:attributes][:minheight]}'"
        end
        if @args[:attributes][:layout]
          attributes["data-layout"] = "'#{@args[:attributes][:layout]}'"
        end
        if @args[:attributes][:theme]
          attributes["data-theme"] = "'#{@args[:attributes][:theme]}'"
        end
        if @args[:flags][:autorun]
          attributes["data-autorun"] = ""
        end
        attrs = ""
        attributes.each {|key, value|
          if value.empty?
            attrs << "#{key} "
          else
            attrs << "#{key}=#{value} "
          end
        }
        attrs.rstrip
      end
    end

    class ScalaFiddleIntegration
      BODY_END_TAG = %r!</body>!

      class << self
        def append_scalafiddle_code(doc)
          @config = doc.site.config
          if doc.output =~ BODY_END_TAG
            # Insert code before body's end if this document has one.
            location = doc.output.index(BODY_END_TAG)
            doc.output = doc.output.slice(0, location) + api_code(doc) + doc.output.slice(location, doc.output.length - location)
          else
            doc.output.prepend(api_code(doc))
          end
        end

        private
        def load_template(template, dir)
          file = dir + "/" + template + ".scala"
          content = File.readlines(file)
          if content.index {|l| l.start_with?("////")} == nil
            raise RuntimeException, "Template is missing a //// marker"
          end
          {
              :name => template,
              :pre => content.take_while {|l| !l.start_with?("////")},
              :post => content.drop_while {|l| !l.start_with?("////")}.drop(1)
          }
        end

        private
        def escape_string(strs)
          strs.join.gsub(/\\/, "\\\\\\\\").gsub(/\n/, "\\n").gsub(/\r/, "").gsub(/\t/, "\\t").gsub(/'/) {|m| "\\'"}
        end

        private
        def api_code(page)
          result = ""
          if page.output =~ /<div data-scalafiddle=""/
            templates = page.output.scan(/<div data-scalafiddle="" data-template="([^"]+)"/).flatten
            unless templates.empty?
              result += %Q(
<script>
  window.scalaFiddleTemplates = {
)
              dir = page.site.source + "/" + @config.fetch("scalafiddle", {}).fetch("templateDir", "_scalafiddle")
              js_code = templates.map {|template| load_template(template, dir)}
              result += js_code.map {|template|
                %Q(
    '#{template[:name]}': {
      pre: '#{escape_string(template[:pre])}',
      post: '#{escape_string(template[:post])}'
    }
)
              }.join(",\n")
              result += %Q(
  }
</script>
)
            end
            result += %Q(
<script defer src='#{@config.fetch("scalafiddle", {}).fetch("scalaFiddleUrl", "https://embed.scalafiddle.io/")}integration.js'></script>
)
          end
          result
        end
      end
    end
  end
end

Liquid::Template.register_tag("scalafiddle", Jekyll::ScalaFiddle::ScalaFiddleTag)

Jekyll::Hooks.register [:pages, :documents], :post_render do |doc|
  Jekyll::ScalaFiddle::ScalaFiddleIntegration.append_scalafiddle_code(doc)
end

