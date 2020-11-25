# Lossless Video Cutter

Designed to cut video keeping the quality of the original video.

## How is this working?

The output video is a byte-copy of a specified region of the original video. This approach avoids decoding and encoding the video, therefore keeping the original quality. As a side-effect, the 'cut' operation is extremely fast and light on the CPU.

Under the hood using [Mobile-FFMpeg](https://github.com/tanersener/mobile-ffmpeg).

## The limitation

The limitation of such an approach - the first frame of the cut video should be a so-called keyframe. Each keyframe stores the complete still image of the frame, while subsequent frames are only encoding the changes to the keyframe. Keyframe may appear each second or so, depending on the recorder. Therefore, if the first frame of a video is not a keyframe, video information cannot be decoded until the next keyframe arises, resulting in a black or broken beginning of the video sequence and audio synchronization issues.

The app forces the beginning of 'cut' selection to be the keyframe.

## License

~~~
Lossless Video Cutter. Designed to cut video keeping the quality of the original video.
Copyright (C) 2020  Mr-Goldberg

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program. If not, see https://www.gnu.org/licenses/gpl-3.0.en.html
~~~

[GNU Lesser General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html)
