import WidgetKit
import SwiftUI

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> SimpleEntry {
        SimpleEntry(date: Date(), widgetCount: 0, topWidget: "Loading...")
    }

    func getSnapshot(in context: Context, completion: @escaping (SimpleEntry) -> ()) {
        let entry = SimpleEntry(date: Date(), widgetCount: 0, topWidget: "Snapshot")
        completion(entry)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> ()) {
        // Read data from UserDefaults shared with Flutter app
        let sharedDefaults = UserDefaults(suiteName: "group.com.stackablewidgets")
        let widgetCount = sharedDefaults?.integer(forKey: "widget_count") ?? 0
        let topWidget = sharedDefaults?.string(forKey: "widget_0_title") ?? "No widgets"

        let entry = SimpleEntry(
            date: Date(),
            widgetCount: widgetCount,
            topWidget: topWidget
        )

        let timeline = Timeline(entries: [entry], policy: .atEnd)
        completion(timeline)
    }
}

struct SimpleEntry: TimelineEntry {
    let date: Date
    let widgetCount: Int
    let topWidget: String
}

struct StackableWidgetEntryView : View {
    var entry: Provider.Entry

    var body: some View {
        ZStack {
            LinearGradient(
                gradient: Gradient(colors: [Color.blue, Color.purple]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )

            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Image(systemName: "square.stack.3d.up.fill")
                        .font(.title2)
                    Text("Stackable Widgets")
                        .font(.headline)
                    Spacer()
                }

                Spacer()

                Text("\(entry.widgetCount)")
                    .font(.system(size: 48, weight: .bold))

                Text(entry.widgetCount == 1 ? "widget" : "widgets")
                    .font(.subheadline)
                    .opacity(0.8)

                if entry.widgetCount > 0 {
                    Divider()
                        .background(Color.white.opacity(0.3))

                    Text("Top: \(entry.topWidget)")
                        .font(.caption)
                        .lineLimit(1)
                }

                Spacer()
            }
            .padding()
            .foregroundColor(.white)
        }
    }
}

@main
struct StackableWidgetExtension: Widget {
    let kind: String = "StackableWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            StackableWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Stackable Widgets")
        .description("View your stackable widgets")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct StackableWidget_Previews: PreviewProvider {
    static var previews: some View {
        StackableWidgetEntryView(entry: SimpleEntry(date: Date(), widgetCount: 5, topWidget: "My Counter"))
            .previewContext(WidgetPreviewContext(family: .systemSmall))
    }
}
